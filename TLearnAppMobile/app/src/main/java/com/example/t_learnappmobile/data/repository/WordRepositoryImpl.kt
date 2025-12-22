package com.example.t_learnappmobile.data.repository


import com.example.t_learnappmobile.domain.model.CardAction
import com.example.t_learnappmobile.domain.model.RotationAction
import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.model.PartOfSpeech
import com.example.t_learnappmobile.model.VocabularyStats
import com.example.t_learnappmobile.model.Word
import kotlinx.coroutines.flow.Flow


class WordRepositoryImpl(
    private val api: WordApi,
    private val storage: WordsStorage
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

    override fun getRotationWords(): List<Word> {
        return storage.getRotationWords()
    }

    override fun addWord(word: Word) {
        storage.addWord(word)
    }


    override suspend fun fetchWordBatch(userId: Int, vocabularyId: Int, batchSize: Int): List<Word>? {
        return try {
            val response = api.getBatch(vocabularyId, batchSize)
            if (response.isSuccessful && response.body() != null) {
                val batch = response.body()!!
                storage.updateWords(batch)
                batch
            } else {
                val mockBatch = getMockBatch(userId, vocabularyId, batchSize)
                storage.updateWords(mockBatch)
                mockBatch
            }
        } catch (e: Exception) {
            val mockBatch = getMockBatch(userId, vocabularyId, batchSize)
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


    private fun getMockBatch(userId: Int, vocabularyId: Int, batchSize: Int): List<Word> {
        val currentDict = ServiceLocator.dictionaryManager.getCurrentDictionary(userId)

        val wordsMap = mapOf(
            1 to listOf("Hello", "Goodbye", "Please", "Thank you", "Sorry"),
            2 to listOf("Computer", "Software", "Internet", "Data", "Algorithm"),
            3 to listOf("Cool", "Awesome", "Bro", "Dude", "Sick")
        )

        val translationsMap = mapOf(
            1 to listOf("Привет", "До свидания", "Пожалуйста", "Спасибо", "Извините"),
            2 to listOf("Компьютер", "ПО", "Интернет", "Данные", "Алгоритм"),
            3 to listOf("Круто", "Отлично", "Братан", "Чувак", "Круто")
        )

        val words = wordsMap[vocabularyId] ?: wordsMap[1]!!
        val translations = translationsMap[vocabularyId] ?: translationsMap[1]!!

        return List(batchSize) { index ->
            Word(
                id = index + 1,
                vocabularyId = vocabularyId,
                englishWord = words.getOrElse(index % words.size) { "Word${index + 1}" },
                transcription = "[wɜːrd]",
                partOfSpeech = PartOfSpeech.NOUN,
                russianTranslation = translations.getOrElse(index % translations.size) { "Слово${index + 1}" },
                category = currentDict.name,
            )
        }
    }

    override suspend fun fetchStats(vocabularyId: Int): VocabularyStats? {
        return null
    }
}