package com.example.t_learnappmobile.data.sync

import android.util.Log
import com.example.t_learnappmobile.data.local.WordLocalSource
import com.example.t_learnappmobile.data.local.entities.UserWordEntity
import com.example.t_learnappmobile.data.remote.FirebaseAuthSource
import com.example.t_learnappmobile.data.remote.FirebaseFirestoreSource
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class SyncManager(
    private val localSource: WordLocalSource,
    private val remoteSource: FirebaseFirestoreSource,
    private val authSource: FirebaseAuthSource
) {
    private val TAG = "SyncManager"
    private val SYNC_INTERVAL_MS = 60_000L

    @Volatile
    private var syncJob: Job? = null

    @Volatile
    private var currentScope: CoroutineScope? = null

    fun startPeriodicSync() {
        stopPeriodicSync()

        val newScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        currentScope = newScope

        syncJob = newScope.launch {
            Log.d(TAG, "Periodic sync started")
            while (isActive) {
                try {
                    delay(SYNC_INTERVAL_MS)
                    syncPendingChanges()
                } catch (e: CancellationException) {
                    Log.d(TAG, "Sync cancelled")
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Sync error", e)
                }
            }
            Log.d(TAG, "Periodic sync ended")
        }
    }

    fun stopPeriodicSync() {
        syncJob?.cancel()
        syncJob = null
        currentScope?.cancel()
        currentScope = null
        Log.d(TAG, "Periodic sync stopped")
    }

    suspend fun syncPendingChanges() {
        val userId = authSource.getCurrentUserId()
        if (userId == null) {
            Log.d(TAG, "User not authenticated, skipping sync")
            return
        }

        try {
            val unsyncedProgress = localSource.getUnsyncedProgress(userId)

            if (unsyncedProgress.isEmpty()) {
                return
            }

            Log.d(TAG, "Syncing ${unsyncedProgress.size} pending changes")

            unsyncedProgress.chunked(5).forEach { batch ->
                batch.forEach { progress ->
                    try {
                        remoteSource.saveUserProgress(progress)
                        localSource.markAsSynced(progress.userId, progress.wordId)
                        Log.d(TAG, "Synced progress for word: ${progress.wordId}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to sync ${progress.wordId}", e)
                    }
                }
                delay(100)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync error", e)
        }
    }

}