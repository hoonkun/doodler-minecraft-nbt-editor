package doodler.minecraft

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.channels.UnresolvedAddressException

@Serializable
data class MinecraftUserProfile(
    val name: String,
    val changedToAt: Long? = null
) {

    companion object {

        suspend fun fetch(uuid: List<String>) = coroutineScope {
            val client = HttpClient(CIO)

            val result = mutableMapOf<String, String>()

            try {

                for (uuidEach in uuid) {
                    val endpoint = "https://api.mojang.com/user/profiles/$uuidEach/names"
                    val response = client.get(endpoint)

                    when (response.status) {
                        HttpStatusCode.OK -> {
                            val responseContent = Json.decodeFromString<List<MinecraftUserProfile>>(response.body())
                            result[uuidEach] = responseContent.last().name
                        }
                        HttpStatusCode.NotFound -> {
                            result[uuidEach] = "<unknown>"
                        }
                        else -> {
                            result[uuidEach] = "<error>"
                        }
                    }
                }

            } catch(e: UnresolvedAddressException) {
                println("It seems there is no internet connection. Check it right away!")
            } catch(e: Exception) {
                e.printStackTrace()
            }

            result
        }

    }

}
