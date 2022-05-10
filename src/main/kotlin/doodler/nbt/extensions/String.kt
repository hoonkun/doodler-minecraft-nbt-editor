package nbt.extensions

fun String.indent() = split("\n").joinToString("\n") { "  $it" }