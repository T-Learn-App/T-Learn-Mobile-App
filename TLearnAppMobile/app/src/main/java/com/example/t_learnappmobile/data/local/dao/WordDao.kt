package com.example.t_learnappmobile.data.local.dao

import androidx.room.*
import com.example.t_learnappmobile.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words WHERE dictionaryId = :dictionaryId")
    suspend fun getWordsByDictionary(dictionaryId: String): List<WordEntity>  // ✅ suspend, не Flow

    @Query("SELECT * FROM words WHERE dictionaryId = :dictionaryId")
    fun getWordsByDictionaryFlow(dictionaryId: String): Flow<List<WordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)

    @Query("SELECT * FROM user_words WHERE userId = :userId AND dictionaryId = :dictionaryId")
    suspend fun getUserWords(userId: String, dictionaryId: String): List<UserWordEntity>  // ✅ suspend, не Flow

    @Query("SELECT * FROM user_words WHERE userId = :userId AND wordId = :wordId")
    suspend fun getUserWord(userId: String, wordId: String): UserWordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserWord(userWord: UserWordEntity)

    @Update
    suspend fun updateUserWord(userWord: UserWordEntity)

    @Query("SELECT * FROM dictionaries ORDER BY `order`")
    suspend fun getDictionaries(): List<DictionaryEntity>  // ✅ suspend, не Flow

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDictionaries(dictionaries: List<DictionaryEntity>)
}