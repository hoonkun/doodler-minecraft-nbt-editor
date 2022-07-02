package doodler.extension

import java.util.*

fun String.toUUID(): UUID? {
    return try { UUID.fromString(this) } catch (e: IllegalArgumentException) { null }
}