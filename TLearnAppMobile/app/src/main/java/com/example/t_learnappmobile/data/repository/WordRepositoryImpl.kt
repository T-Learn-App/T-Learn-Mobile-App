package com.example.t_learnappmobile.data.repository


import com.example.t_learnappmobile.domain.model.CardAction
import com.example.t_learnappmobile.domain.model.RotationAction
import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.model.PartOfSpeech
import com.example.t_learnappmobile.model.VocabularyStats
import com.example.t_learnappmobile.model.Word
import kotlinx.coroutines.flow.Flow




class WordRepositoryImpl (
    private val api: WordApi,
    private val storage : WordsStorage
) : WordRepository {

    override fun nextWord() {
        storage.nextWord()
    }

    override fun getCurrentCardFlow(): Flow<Word?> {
        return storage.currentCardFlow
    }

    override fun getCurrentCard(): Word? {
        return storage.getCurrentWord()
    }
    override fun getNewWords(): List<Word> {
        return storage.getNewWords()
    }

    override fun getLearnedWords(): List<Word> {
        return storage.getLearnedWords()
    }
    override fun getRotationWords() : List<Word> {
        return storage.getRotationWords()
    }

    override fun addWord(word: Word) {
        storage.addWord(word)
    }


    override suspend fun fetchWordBatch(vocabularyId: Int, batchSize: Int): List<Word>? {
        return try {
            val response = api.getBatch(vocabularyId, batchSize)
            if (response.isSuccessful && response.body() != null) {
                val batch = response.body()!!
                storage.updateWords(batch)
                batch
            } else {
                val mockBatch = getMockBatch(vocabularyId, batchSize)
                storage.updateWords(mockBatch)
                mockBatch
            }
        } catch (e: Exception) {
            val mockBatch = getMockBatch(vocabularyId, batchSize)
            storage.updateWords(mockBatch)
            mockBatch
        }
    }


    override suspend fun sendRotationAction(wordId: Int, action: CardAction): Boolean {
        return try {
            val response = api.sendAction(RotationAction(wordId, action.apiKey))
            if (response.isSuccessful) {
                true
            } else {
                println("API Error: ${response.code()} - ${response.message()}")
                true
            }
        } catch (e: Exception) {
            println("Exception in sendRotationAction: ${e.message}")
            e.printStackTrace()
            true
        }
    }


    private fun getMockBatch(vocabularyId: Int, batchSize: Int): List<Word> {
        val words = listOf(
            "Hello", "World", "Book", "House", "School",
            "Teacher", "Student", "Learn", "Study", "Apple"
        )
        val translations = listOf(
            "Привет", "Мир", "Книга", "Дом", "Школа",
            "Учитель", "Студент", "Учиться", "Изучать", "Яблоко"
        )

        return List(batchSize) { index ->
            Word(
                id = index + 1,
                vocabularyId = vocabularyId,
                englishWord = words.getOrElse(index) { "Word${index + 1}" },
                transcription = "[wɜːrd]",
                partOfSpeech = PartOfSpeech.NOUN,
                russianTranslation = translations.getOrElse(index) { "Слово${index + 1}" },
                category = "English Basics",
            )
        }
    }

    override suspend fun fetchStats(vocabularyId: Int): VocabularyStats? {
        return null
    }
}