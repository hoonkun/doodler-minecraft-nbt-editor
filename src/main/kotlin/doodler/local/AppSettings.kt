package doodler.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private val AppSettingsPath = System.getProperty("user.home").plus("/.local/share/doodler/preferences.json")

var UserAppSettings: AppSettings = readAppSettings()

private val DefaultAppSettings = AppSettings(
    globalScale = 1.0f
)

fun readAppSettings(): AppSettings {
    val file = File(AppSettingsPath)
    if (!file.exists()) {
        file.parentFile.mkdirs()
        file.writeText(Json.encodeToString(DefaultAppSettings))
        return DefaultAppSettings
    }

    val raw = file.readText()
    return try { Json.decodeFromString(raw) } catch (e: Exception) { DefaultAppSettings }
}

fun saveAppSettings(newSettings: AppSettings) {
    val file = File(AppSettingsPath)
    if (!file.exists()) file.parentFile.mkdirs()

    file.writeText(Json.encodeToString(newSettings))
    UserAppSettings = newSettings
}

@Serializable
data class AppSettings(
    val globalScale: Float
)
