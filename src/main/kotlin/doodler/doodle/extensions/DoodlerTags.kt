package doodler.doodle.extensions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import doodler.doodle.ActualDoodle
import doodler.doodle.InternalAssertionException
import doodler.doodle.NbtDoodle
import doodler.doodle.ValueDoodle
import composables.global.ThemedColor
import doodler.nbt.TagType
import doodler.nbt.tag.*


fun CompoundTag.doodle(parent: NbtDoodle?, depth: Int): List<ActualDoodle> {
    return this.value.map { NbtDoodle(it, depth, parent) }
}

fun ListTag.doodle(parent: NbtDoodle, depth: Int): List<ActualDoodle> {
    return this.value.map { NbtDoodle(it, depth, parent) }
}

fun ByteArrayTag.doodle(parent: NbtDoodle, depth: Int): List<ActualDoodle> {
    return this.value.map { ValueDoodle("$it", depth, parent) }
}

fun IntArrayTag.doodle(parent: NbtDoodle, depth: Int): List<ActualDoodle> {
    return this.value.map { ValueDoodle("$it", depth, parent) }
}

fun LongArrayTag.doodle(parent: NbtDoodle, depth: Int): List<ActualDoodle> {
    return this.value.map { ValueDoodle("$it", depth, parent) }
}

fun TagType.shorten(): String {
    return when (this) {
        TagType.TAG_BYTE -> " B "
        TagType.TAG_SHORT -> " S "
        TagType.TAG_INT -> " I "
        TagType.TAG_LONG -> " L "
        TagType.TAG_FLOAT -> " F "
        TagType.TAG_DOUBLE -> " D "
        TagType.TAG_BYTE_ARRAY -> "[B]"
        TagType.TAG_INT_ARRAY -> "[I]"
        TagType.TAG_LONG_ARRAY -> "[L]"
        TagType.TAG_STRING -> "..."
        TagType.TAG_COMPOUND -> "{ }"
        TagType.TAG_LIST -> "[ ]"
        TagType.TAG_END -> "   "
    }
}

fun TagType.color(): Color {
    return if (this.isNumber()) ThemedColor.Editor.Tag.Number
    else if (this.isString()) ThemedColor.Editor.Tag.String
    else if (this.isArray()) ThemedColor.Editor.Tag.NumberArray
    else if (this.isList()) ThemedColor.Editor.Tag.List
    else if (this.isCompound()) ThemedColor.Editor.Tag.Compound
    else ThemedColor.Editor.Tag.General
}

fun TagType.creationHint(): String {
    return when (this) {
        TagType.TAG_BYTE -> "Byte (in [-128, 127])"
        TagType.TAG_SHORT -> "Short (in [-32768, 32767])"
        TagType.TAG_INT -> "Integer"
        TagType.TAG_LONG -> "Long"
        TagType.TAG_FLOAT -> "Float"
        TagType.TAG_DOUBLE -> "Double"
        TagType.TAG_BYTE_ARRAY -> "creates empty ByteArray tag"
        TagType.TAG_INT_ARRAY -> "creates empty IntArray tag"
        TagType.TAG_LONG_ARRAY -> "creates empty LongArray tag"
        TagType.TAG_STRING -> "String (text)"
        TagType.TAG_COMPOUND -> "creates empty Compound tag"
        TagType.TAG_LIST -> "creates empty List tag"
        TagType.TAG_END -> ""
    }
}

fun transformText(text: String, valid: Boolean): TransformedText {
    return if (!valid) {
        TransformedText(AnnotatedString(
            text,
            listOf(AnnotatedString.Range(SpanStyle(color = ThemedColor.Editor.Selector.Invalid), 0, text.length))
        ), OffsetMapping.Identity)
    } else {
        TransformedText(AnnotatedString(text), OffsetMapping.Identity)
    }
}

private fun createTransformer(
    validator: (AnnotatedString) -> Boolean
): (AnnotatedString) -> Pair<Boolean, TransformedText> = { input ->
    val valid = validator(input)
    Pair(valid, transformText(input.text, valid))
}

private val byteValidator: (input: AnnotatedString) -> Boolean = { it.text.toByteOrNull() != null }

private val shortValidator: (input: AnnotatedString) -> Boolean = { it.text.toShortOrNull() != null }

private val intValidator: (input: AnnotatedString) -> Boolean = { it.text.toIntOrNull() != null }

private val longValidator: (input: AnnotatedString) -> Boolean = { it.text.toLongOrNull() != null }

private val floatValidator: (input: AnnotatedString) -> Boolean = { it.text.toFloatOrNull() != null }

private val doubleValidator: (input: AnnotatedString) -> Boolean = { it.text.toDoubleOrNull() != null }

private val byteTransformer = createTransformer(byteValidator)

private val shortTransformer = createTransformer(shortValidator)

private val intTransformer = createTransformer(intValidator)

private val longTransformer = createTransformer(longValidator)

private val floatTransformer = createTransformer(floatValidator)

private val doubleTransformer = createTransformer(doubleValidator)

private val stringTransformer = createTransformer { true }

fun TagType.transformer(): (AnnotatedString) -> Pair<Boolean, TransformedText> {
    return when(this) {
        TagType.TAG_SHORT -> shortTransformer
        TagType.TAG_LONG -> longTransformer
        TagType.TAG_INT -> intTransformer
        TagType.TAG_FLOAT -> floatTransformer
        TagType.TAG_BYTE -> byteTransformer
        TagType.TAG_DOUBLE -> doubleTransformer
        TagType.TAG_STRING -> stringTransformer
        else -> throw InternalAssertionException(listOf("Numeric Tag", "String Tag"), "$this")
    }
}

fun TagType.displayName(): String {
    return when (this) {
        TagType.TAG_END -> "noop"
        TagType.TAG_COMPOUND -> "compound"
        TagType.TAG_LIST -> "list"
        TagType.TAG_LONG_ARRAY -> "long array"
        TagType.TAG_INT_ARRAY -> "int array"
        TagType.TAG_BYTE_ARRAY -> "byte array"
        TagType.TAG_STRING -> "string"
        TagType.TAG_SHORT -> "short"
        TagType.TAG_LONG -> "long"
        TagType.TAG_INT -> "int"
        TagType.TAG_FLOAT -> "float"
        TagType.TAG_BYTE -> "byte"
        TagType.TAG_DOUBLE -> "double"
    }
}