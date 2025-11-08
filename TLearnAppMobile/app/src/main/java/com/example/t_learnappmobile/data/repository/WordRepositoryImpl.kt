package com.example.t_learnappmobile.data.repository

import android.util.Log
import com.example.t_learnappmobile.domain.model.RotationAction
import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.model.CardType
import com.example.t_learnappmobile.model.PartOfSpeech
import com.example.t_learnappmobile.model.VocabularyStats
import com.example.t_learnappmobile.model.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface WordApi {
    @GET("words")
    suspend fun getBatch(
        @Query("vocabulary_id") vocabularyId: Int,
        @Query("batch_size") batchSize: Int
    ): Response<List<Word>>

    @POST("rotation")
    suspend fun sendAction(@Body action: RotationAction): Response<Unit>
}

class WordRepositoryImpl : WordRepository {
    private val wordsStorage = mutableListOf<Word>()
    private val _wordsUpdate = MutableStateFlow(0L)

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:8080/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(WordApi::class.java)

    override fun getCurrentCardFlow(): Flow<Word?> = _wordsUpdate.map { getCurrentCard() }

    override fun getCurrentCard(): Word? = wordsStorage.firstOrNull()

    override fun getNewWords(): List<Word> = wordsStorage.filter { it.cardType == CardType.NEW }

    override fun getLearnedWords(): List<Word> = wordsStorage.filter { it.isLearned }

    override fun addWord(word: Word) {
        wordsStorage.add(word)
        triggerUpdate()
    }

    override fun triggerUpdate() {
        _wordsUpdate.value = System.currentTimeMillis()
    }

    override suspend fun fetchWordBatch(vocabularyId: Int, batchSize: Int): List<Word>? {
        return try {
            val response = api.getBatch(vocabularyId, batchSize)
            if (response.isSuccessful && response.body() != null) {
                val batch = response.body()!!
                wordsStorage.clear()
                wordsStorage.addAll(batch)
                triggerUpdate()
                batch
            } else {
                fallbackToMock(vocabularyId, batchSize)
            }
        } catch (e: Exception) {
            fallbackToMock(vocabularyId, batchSize)
        }
    }

    private fun fallbackToMock(vocabularyId: Int, batchSize: Int): List<Word> {
        val mockBatch = getMockBatch(vocabularyId, batchSize)
        wordsStorage.clear()
        wordsStorage.addAll(mockBatch)
        triggerUpdate()
        return mockBatch
    }

    override suspend fun sendRotationAction(wordId: Int, action: String): Boolean {
        return try {
            val response = api.sendAction(RotationAction(wordId, action))
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private fun getMockBatch(vocabularyId: Int, batchSize: Int): List<Word> {
        return List(batchSize) { index ->
            Word(
                id = index + 1,
                vocabularyId = vocabularyId,
                englishWord = "Hello",
                transcription = "[wɜːrd]",
                partOfSpeech = PartOfSpeech.NOUN,
                russianTranslation = "Привет",
                category = "English Basics",
            )
        }
    }

    override suspend fun fetchStats(vocabularyId: Int): VocabularyStats? {
        return null
    }
}
