// data/sync/SyncManager.kt
package com.example.t_learnappmobile.data.sync

import android.util.Log
import com.example.t_learnappmobile.data.local.WordLocalSource
import com.example.t_learnappmobile.data.remote.FirebaseAuthSource
import com.example.t_learnappmobile.data.remote.FirebaseFirestoreSource
import kotlinx.coroutines.*

class SyncManager(
    private val localSource: WordLocalSource,
    private val remoteSource: FirebaseFirestoreSource,
    private val authSource: FirebaseAuthSource  // Сохраняем как private val
) {
    private val TAG = "SyncManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null

    fun startPeriodicSync() {
        syncJob?.cancel()
        syncJob = scope.launch {
            while (isActive) {
                delay(60_000)
                syncPendingChanges()
            }
        }
        Log.d(TAG, "Periodic sync started")
    }

    fun stopPeriodicSync() {
        syncJob?.cancel()
        syncJob = null
        scope.cancel()  // Закрываем scope
        Log.d(TAG, "Periodic sync stopped")
    }

    suspend fun syncPendingChanges() {
        try {
            val unsyncedProgress = getAllUnsyncedProgress()

            if (unsyncedProgress.isEmpty()) {
                return
            }

            Log.d(TAG, "Syncing ${unsyncedProgress.size} pending changes")

            unsyncedProgress.forEach { progress ->
                try {
                    remoteSource.saveUserProgress(progress)
                    localSource.markAsSynced(progress.userId, progress.wordId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync ${progress.wordId}", e)
                }
            }
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

    private suspend fun getAllUnsyncedProgress(): List<com.example.t_learnappmobile.data.local.entities.UserWordEntity> {
        val userId = authSource.getCurrentUserId() ?: return emptyList()
        return localSource.getUnsyncedProgress(userId)
    }
}