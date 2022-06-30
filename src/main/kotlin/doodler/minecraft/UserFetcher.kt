package doodler.minecraft

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class MinecraftUserProfile(
    val name: String,
    val changedToAt: Long? = null
) {

    companion object {

        suspend fun fetch(uuid: List<String>) = coroutineScope {
            val client = HttpClient(CIO)

            val result = mutableMapOf<String, String>()

            for (uuidEach in uuid) {
                val endpoint = "https://api.mojang.com/user/profiles/$uuidEach/names"
                val rawResponse = client.get(endpoint).body<String>()
                val response = Json.decodeFromString<List<MinecraftUserProfile>>(rawResponse)

                result[uuidEach] = response.last().name
            }

            result
        }

    }

}
