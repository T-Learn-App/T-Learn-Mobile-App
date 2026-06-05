package com.example.t_learnappmobile.data.sync

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.t_learnappmobile.data.local.GameLocalSource
import com.example.t_learnappmobile.data.local.PendingGameResult
import com.example.t_learnappmobile.data.local.WordLocalSource
import com.example.t_learnappmobile.data.remote.FirebaseAuthSource
import com.example.t_learnappmobile.data.remote.FirebaseFirestoreSource
import com.example.t_learnappmobile.data.remote.FirebaseGameSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SyncManager(
    private val context: Context,
    private val localSource: WordLocalSource,
    private val remoteSource: FirebaseFirestoreSource,
    private val authSource: FirebaseAuthSource,
    private val gameLocalSource: GameLocalSource,
    private val firebaseGameSource: FirebaseGameSource
) {
    private val TAG = "SyncManager"
    private val SYNC_INTERVAL_MS = 30_000L

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()



    @Volatile
    private var syncJob: Job? = null

    @Volatile
    private var isSyncing = false

    fun startPeriodicSync() {
        stopPeriodicSync()

        syncJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            Log.d(TAG, "Periodic sync started (interval: ${SYNC_INTERVAL_MS}ms)")

            while (isActive) {
                delay(SYNC_INTERVAL_MS)
                if (isNetworkAvailable()) {
                    Log.d(TAG, "Periodic sync triggered")
                    syncPendingChanges()
                    syncPendingGames()
                }
            }
        }
    }

    fun stopPeriodicSync() {
        syncJob?.cancel()
        syncJob = null
        Log.d(TAG, "Periodic sync stopped")
    }


    suspend fun syncPendingChanges(): SyncResult {
        if (isSyncing) return SyncResult.InProgress
        if (!isNetworkAvailable()) return SyncResult.NoInternet

        val userId = authSource.getCurrentUserId()
        if (userId == null) {
            Log.d(TAG, "User not authenticated, skipping sync")
            return SyncResult.NotAuthenticated
        }

        isSyncing = true
        _syncState.value = SyncState.Synching

        return try {
            val unsyncedProgress = localSource.getUnsyncedProgress(userId)

            if (unsyncedProgress.isEmpty()) {
                Log.d(TAG, "No pending changes to sync")
                _syncState.value = SyncState.Idle
                return SyncResult.NoChanges
            }

            Log.d(TAG, "Syncing ${unsyncedProgress.size} pending changes")

            var successCount = 0
            var failCount = 0

            unsyncedProgress.chunked(5).forEach { batch ->
                batch.forEach { progress ->
                    try {
                        withTimeoutOrNull(3000L) {
                            remoteSource.saveUserProgress(progress)
                            localSource.markAsSynced(progress.userId, progress.wordId)
                        }
                        successCount++
                        Log.d(TAG, "Synced progress for word: ${progress.wordId}, stage: ${progress.stage}")
                    } catch (e: Exception) {
                        failCount++
                        Log.e(TAG, "Failed to sync ${progress.wordId}: ${e.message}")
                    }
                }
                delay(100)
            }

            val result = if (failCount == 0) {
                Log.d(TAG, "Sync completed successfully: $successCount items")
                _syncState.value = SyncState.Success(successCount)
                SyncResult.Success(successCount)
            } else {
                Log.w(TAG, "Sync partially completed: $successCount success, $failCount failed")
                _syncState.value = SyncState.PartialSuccess(successCount, failCount)
                SyncResult.PartialSuccess(successCount, failCount)
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Sync error", e)
            _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            SyncResult.Error(e.message ?: "Unknown error")
        } finally {
            isSyncing = false
        }
    }

    suspend fun syncPendingGames(): SyncResult {
        if (isSyncing) return SyncResult.InProgress
        if (!isNetworkAvailable()) return SyncResult.NoInternet

        val userId = authSource.getCurrentUserId()
        if (userId == null) {
            Log.d(TAG, "User not authenticated, skipping game sync")
            return SyncResult.NotAuthenticated
        }

        try {
            val pendingResults = gameLocalSource.getPendingGameResults()
            if (pendingResults.isEmpty()) {
                Log.d(TAG, "No pending game results to sync")
                return SyncResult.NoChanges
            }

            Log.d(TAG, "Found ${pendingResults.size} pending game results to sync")

            val syncedResults = mutableListOf<PendingGameResult>()
            var totalScore = 0

            for (result in pendingResults) {
                try {
                    withTimeoutOrNull(3000L) {
                        firebaseGameSource.saveGameResult(result.userId, result.score, result.wordsCount)
                        remoteSource.updateScore(result.userId, result.score)
                    }
                    totalScore += result.score
                    syncedResults.add(result)
                    Log.d(TAG, "Synced game result: score=${result.score}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync game result", e)
                }
            }

            if (syncedResults.isNotEmpty()) {
                gameLocalSource.removePendingGameResults(syncedResults)
                Log.d(TAG, "Removed ${syncedResults.size} synced game results, total score added: $totalScore")
            }

            return if (syncedResults.size == pendingResults.size) {
                SyncResult.Success(syncedResults.size)
            } else if (syncedResults.isNotEmpty()) {
                SyncResult.PartialSuccess(syncedResults.size, pendingResults.size - syncedResults.size)
            } else {
                SyncResult.Error("Failed to sync game results")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing pending games", e)
            return SyncResult.Error(e.message ?: "Unknown error")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

sealed class SyncState {
    object Idle : SyncState()
    object Synching : SyncState()
    data class Success(val count: Int) : SyncState()
    data class PartialSuccess(val successCount: Int, val failCount: Int) : SyncState()
    data class Error(val message: String) : SyncState()
}

sealed class SyncResult {
    object NoChanges : SyncResult()
    object NotAuthenticated : SyncResult()
    object InProgress : SyncResult()
    object NoInternet : SyncResult()
    data class Success(val count: Int) : SyncResult()
    data class PartialSuccess(val successCount: Int, val failCount: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}