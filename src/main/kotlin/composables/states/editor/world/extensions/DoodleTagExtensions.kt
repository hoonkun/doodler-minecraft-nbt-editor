package composables.states.editor.world.extensions

import androidx.compose.ui.graphics.Color
import composables.states.editor.world.ActualDoodle
import composables.states.editor.world.NbtDoodle
import composables.states.editor.world.ValueDoodle
import composables.themed.ThemedColor
import doodler.nbt.TagType
import doodler.nbt.tag.*


fun CompoundTag.doodle(parent: NbtDoodle?, depth: Int): List<ActualDoodle> {
    return this.value.mapIndexed { index, value -> NbtDoodle(value, depth, index, parent) }
}

fun ListTag.doodle(parent: NbtDoodle, depth: Int): List<ActualDoodle> {
    return this.value.mapIndexed { index, value -> NbtDoodle(value, depth, index, parent) }
}

fun ByteArrayTag.doodle(parent: NbtDoodle, depth: Int): List<ActualDoodle> {
    return this.value.mapIndexed { index, value ->
        ValueDoodle("$value", depth, index, parent)
    }
}

fun IntArrayTag.doodle(parent: NbtDoodle, depth: Int): List<ActualDoodle> {
    return this.value.mapIndexed { index, value ->
        ValueDoodle("$value", depth, index, parent)
    }
}

fun LongArrayTag.doodle(parent: NbtDoodle, depth: Int): List<ActualDoodle> {
    return this.value.mapIndexed { index, value ->
        ValueDoodle("$value", depth, index, parent)
    }
}

fun TagType.shorten(): String {
    return when (this) {
        TagType.TAG_BYTE -> " B "
        TagType.TAG_INT -> " I "
        TagType.TAG_SHORT -> " S "
        TagType.TAG_FLOAT -> " F "
        TagType.TAG_DOUBLE -> " D "
        TagType.TAG_LONG -> " L "
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