// data/local/dao/WordDao.kt
package com.example.t_learnappmobile.data.local.dao

import androidx.room.*
import com.example.t_learnappmobile.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    // ============ Words ============
    @Query("SELECT * FROM words WHERE dictionaryId = :dictionaryId")
    suspend fun getWordsByDictionary(dictionaryId: String): List<WordEntity>

    @Query("SELECT * FROM words WHERE dictionaryId = :dictionaryId")
    fun getWordsByDictionaryFlow(dictionaryId: String): Flow<List<WordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)

    @Query("DELETE FROM words WHERE dictionaryId = :dictionaryId")
    suspend fun deleteWordsByDictionary(dictionaryId: String)

    // ============ User Progress ============
    @Query("SELECT * FROM user_words WHERE userId = :userId AND dictionaryId = :dictionaryId")
    suspend fun getUserWords(userId: String, dictionaryId: String): List<UserWordEntity>

    @Query("SELECT * FROM user_words WHERE userId = :userId AND wordId = :wordId")
    suspend fun getUserWord(userId: String, wordId: String): UserWordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserWord(userWord: UserWordEntity)

    @Update
    suspend fun updateUserWord(userWord: UserWordEntity)

    @Query("SELECT * FROM user_words WHERE userId = :userId AND isSynced = 0")
    suspend fun getUnsyncedUserWords(userId: String): List<UserWordEntity>

    @Query("UPDATE user_words SET isSynced = 1, updatedAt = :timestamp WHERE userId = :userId AND wordId = :wordId")
    suspend fun markAsSynced(userId: String, wordId: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM user_words WHERE userId = :userId AND dictionaryId = :dictionaryId")
    suspend fun deleteUserWordsByDictionary(userId: String, dictionaryId: String)

    // ============ Dictionaries ============
    @Query("SELECT * FROM dictionaries ORDER BY `order`")
    suspend fun getDictionaries(): List<DictionaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDictionaries(dictionaries: List<DictionaryEntity>)
}