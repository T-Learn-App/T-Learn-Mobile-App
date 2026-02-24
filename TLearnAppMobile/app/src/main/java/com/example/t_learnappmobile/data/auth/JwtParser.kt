// data/auth/JwtParser.kt
import android.util.Base64
import org.json.JSONObject
import java.lang.IllegalArgumentException

object JwtParser {

    fun decodePayload(token: String): Map<String, Any?>? {
        return try {
            val payload = token.split(".")[1]  // Берем вторую часть
            val decodedPayload = decodeBase64(payload.padEnd(paddingLength(payload), '='))
            JSONObject(decodedPayload).toMap()
        } catch (e: Exception) {
            null
        }
    }

    private fun paddingLength(payload: String): Int {
        val mod = payload.length % 4
        return if (mod == 0) 0 else 4 - mod
    }

    private fun decodeBase64(base64: String): String {
        val decodedBytes = Base64.decode(base64, Base64.URL_SAFE or Base64.NO_WRAP)
        return String(decodedBytes, Charsets.UTF_8)
    }

    private fun JSONObject.toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val keys = this.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = this[key]
            map[key] = when (value) {
                is JSONObject -> value.toMap()
                else -> value
            }
        }
        return map
    }
}
