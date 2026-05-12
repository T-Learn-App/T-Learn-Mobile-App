package com.example.t_learnappmobile.data.sync

import android.content.Context
import android.util.Log
import com.example.t_learnappmobile.data.local.AppDatabase
import com.example.t_learnappmobile.data.local.entities.DictionaryEntity
import com.example.t_learnappmobile.data.local.entities.UserWordEntity
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class SyncManager(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val wordDao = database.wordDao()
    private val firestore = Firebase.firestore
    private val TAG = "SyncManager"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null

    fun startPeriodicSync() {
        syncJob?.cancel()
        syncJob = scope.launch {
            while (isActive) {
                delay(60_000) // Синхронизация каждую минуту
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
        val userId = ServiceLocator.firebaseAuthManager.getUserId()
        if (userId == null) {
            Log.d(TAG, "User not authenticated, skipping sync")
            return
        }

        try {
            val unsynced = wordDao.getUnsyncedUserWords(userId)
            if (unsynced.isEmpty()) {
                Log.d(TAG, "No pending changes to sync")
                return
            }

            Log.d(TAG, "Syncing ${unsynced.size} pending changes...")

            for (userWord in unsynced) {
                try {
                    val userWordDocId = "${userId}_${userWord.wordId}"
                    val docRef = firestore.collection("user_words").document(userWordDocId)

                    val updates = mapOf(
                        "stage" to userWord.stage,
                        "nextReviewDate" to userWord.nextReviewDate,
                        "failCount" to userWord.failCount,
                        "lastReviewDate" to userWord.lastReviewDate,
                        "totalViews" to userWord.totalViews,
                        "correctCount" to userWord.correctCount,
                        "incorrectCount" to userWord.incorrectCount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )

                    docRef.set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
                    wordDao.markAsSynced(userId, userWord.wordId)
                    Log.d(TAG, "Synced word: ${userWord.wordId}")

                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync word ${userWord.wordId}", e)
                }
            }

            Log.d(TAG, "Sync completed")
        } catch (e: Exception) {
            Log.e(TAG, "Sync error", e)
        }
    }

    suspend fun syncAllData() {
        val userId = ServiceLocator.firebaseAuthManager.getUserId()
        if (userId == null) return

        try {
            // Синхронизируем словари
            val dictionariesSnapshot = firestore.collection("dictionaries")
                .orderBy("order")
                .get()
                .await()

            val dictionaries = dictionariesSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                DictionaryEntity(
                    id = doc.id,
                    name = data["name"] as? String ?: "",
                    order = (data["order"] as? Long)?.toInt() ?: 0
                )
            }
            wordDao.insertDictionaries(dictionaries)

            // Синхронизируем прогресс из Firebase в Room
            val userWordsSnapshot = firestore.collection("user_words")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            for (doc in userWordsSnapshot.documents) {
                val data = doc.data ?: continue
                val userWord = UserWordEntity(
                    userId = userId,
                    wordId = data["wordId"] as? String ?: continue,
                    dictionaryId = data["dictionaryId"] as? String ?: "",
                    stage = (data["stage"] as? Long)?.toInt() ?: 0,
                    nextReviewDate = data["nextReviewDate"] as? Long ?: 0,
                    failCount = (data["failCount"] as? Long)?.toInt() ?: 0,
                    lastReviewDate = data["lastReviewDate"] as? Long,
                    totalViews = (data["totalViews"] as? Long)?.toInt() ?: 0,
                    correctCount = (data["correctCount"] as? Long)?.toInt() ?: 0,
                    incorrectCount = (data["incorrectCount"] as? Long)?.toInt() ?: 0,
                    isSynced = true
                )
                wordDao.insertUserWord(userWord)
            }

            Log.d(TAG, "Full sync completed")
        } catch (e: Exception) {
            Log.e(TAG, "Full sync error", e)
        }
    }
}