package composables.states.editor.world.extensions

import composables.states.editor.world.ActualDoodle
import composables.states.editor.world.NbtDoodle
import composables.states.editor.world.ValueDoodle
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