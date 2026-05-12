// data/local/WordLocalSource.kt
package com.example.t_learnappmobile.data.local

import com.example.t_learnappmobile.data.local.dao.WordDao
import com.example.t_learnappmobile.data.local.entities.*
import kotlinx.coroutines.flow.Flow

class WordLocalSource(private val wordDao: WordDao) {

    suspend fun getWords(dictionaryId: String): List<WordEntity> =
        wordDao.getWordsByDictionary(dictionaryId)

    fun getWordsFlow(dictionaryId: String): Flow<List<WordEntity>> =
        wordDao.getWordsByDictionaryFlow(dictionaryId)

    suspend fun insertWords(words: List<WordEntity>) = wordDao.insertWords(words)

    suspend fun getUserProgress(userId: String, dictionaryId: String): List<UserWordEntity> =
        wordDao.getUserWords(userId, dictionaryId)

    suspend fun getUserWord(userId: String, wordId: String): UserWordEntity? =
        wordDao.getUserWord(userId, wordId)

    suspend fun saveUserProgress(userWord: UserWordEntity) = wordDao.insertUserWord(userWord)

    suspend fun getUnsyncedProgress(userId: String): List<UserWordEntity> =
        wordDao.getUnsyncedUserWords(userId)

    suspend fun markAsSynced(userId: String, wordId: String) =
        wordDao.markAsSynced(userId, wordId)

    suspend fun deleteProgressByDictionary(userId: String, dictionaryId: String) =
        wordDao.deleteUserWordsByDictionary(userId, dictionaryId)

    suspend fun getDictionaries(): List<DictionaryEntity> = wordDao.getDictionaries()

    suspend fun insertDictionaries(dictionaries: List<DictionaryEntity>) =
        wordDao.insertDictionaries(dictionaries)
}