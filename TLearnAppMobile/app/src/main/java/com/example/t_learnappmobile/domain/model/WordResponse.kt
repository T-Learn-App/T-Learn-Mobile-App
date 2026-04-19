import com.google.gson.annotations.SerializedName
import com.example.t_learnappmobile.domain.model.CardAction


data class ListWordResponse(
    val words: List<WordResponse>
)

data class WordResponse(
    val id: Long,
    @SerializedName("part_of_speech")
    val partOfSpeech: String,
    @SerializedName("category_id")
    val category: Long,
    val word: String,
    val transcription: String,
    val translation: String? = "перевод"
)

data class StatQueueDto(
    val wordId: Long,
    val action: String
) {
    constructor(wordId: Long, action: CardAction) : this(wordId, action.apiKey)
}
