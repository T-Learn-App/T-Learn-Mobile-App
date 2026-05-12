// data/sync/SyncManager.kt
package com.example.t_learnappmobile.data.sync

import android.util.Log
import com.example.t_learnappmobile.data.local.WordLocalSource
import com.example.t_learnappmobile.data.remote.FirebaseFirestoreSource
import kotlinx.coroutines.*

class SyncManager(
    private val localSource: WordLocalSource,
    private val remoteSource: FirebaseFirestoreSource
) {
    private val TAG = "SyncManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null

    fun startPeriodicSync() {
        syncJob?.cancel()
        syncJob = scope.launch {
            while (isActive) {
                delay(60_000) // Sync every minute
                syncPendingChanges()
            }
        }
        Log.d(TAG, "Periodic sync started")
    }

    fun stopPeriodicSync() {
        syncJob?.cancel()
        syncJob = null
        Log.d(TAG, "Periodic sync stopped")
    }

    suspend fun syncPendingChanges() {
        try {
            // Sync all unsynced words for all users (we'll get from local source)
            // In a real app, you'd need to get the current user ID
            val unsyncedProgress = getAllUnsyncedProgress()

            if (unsyncedProgress.isEmpty()) {
                Log.d(TAG, "No pending changes to sync")
                return
            }

            Log.d(TAG, "Syncing ${unsyncedProgress.size} pending changes")

            unsyncedProgress.forEach { progress ->
                try {
                    remoteSource.saveUserProgress(progress)
                    localSource.markAsSynced(progress.userId, progress.wordId)
                    Log.d(TAG, "Synced: ${progress.wordId}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync ${progress.wordId}", e)
                }
            }

            Log.d(TAG, "Sync completed")
        } catch (e: Exception) {
            Log.e(TAG, "Sync error", e)
        }
    }

    suspend fun syncAllData() {
        try {
            val dicts = remoteSource.getDictionaries()
            if (dicts.isNotEmpty()) {
                localSource.insertDictionaries(dicts)
            }
            Log.d(TAG, "Full sync completed")
        } catch (e: Exception) {
            Log.e(TAG, "Full sync error", e)
        }
    }

    private suspend fun getAllUnsyncedProgress() = localSource.getUnsyncedProgress("")
}