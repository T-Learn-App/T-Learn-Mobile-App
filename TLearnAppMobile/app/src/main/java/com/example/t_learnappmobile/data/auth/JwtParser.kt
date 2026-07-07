import JwtParser.decodePayload
import android.util.Base64
import org.json.JSONObject

object JwtParser {

    fun decodePayload(token: String): Map<String, Any?>? {
        return try {
            val cleanToken = token.trim().replace(Regex("\\s"), "")

            val parts = cleanToken.split(".")
            if (parts.size != 3) {
                android.util.Log.e("JwtParser", "Invalid token parts: ${parts.size}")
                return null
            }

            val payload = parts[1]
            val decodedPayload = decodeBase64(payload)
            android.util.Log.d("JwtParser", "Decoded payload: $decodedPayload")

            JSONObject(decodedPayload).toMap()
        } catch (e: Exception) {
            android.util.Log.e("JwtParser", "Error decoding token", e)
            null
        }
    }

    private fun decodeBase64(base64: String): String {
        var padded = base64
        when (base64.length % 4) {
            2 -> padded = base64 + "=="
            3 -> padded = base64 + "="
        }

        val decodedBytes = Base64.decode(padded, Base64.URL_SAFE)
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
