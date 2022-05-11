package doodler.doodle

import nbt.AnyTag
import nbt.TagType
import nbt.tag.*

fun CompoundTag.doodle(depth: Int): List<Doodle> {
    return this.value.values.map { NbtDoodle(depth, it) }
}

fun ListTag.doodle(depth: Int): List<Doodle> {
    return this.value.mapIndexed { index, value -> NbtDoodle(depth, value, index) }
}

fun ByteArrayTag.doodle(depth: Int, parentKey: String): List<Doodle> {
    return this.value.mapIndexed { index, value ->
        PrimitiveValueDoodle(depth, "$value", "$parentKey[$index]", index)
    }
}

fun IntArrayTag.doodle(depth: Int, parentKey: String): List<Doodle> {
    return this.value.mapIndexed { index, value ->
        PrimitiveValueDoodle(depth, "$value", "$parentKey[$index]", index)
    }
}

fun LongArrayTag.doodle(depth: Int, parentKey: String): List<Doodle> {
    return this.value.mapIndexed { index, value ->
        PrimitiveValueDoodle(depth, "$value", "$parentKey[$index]", index)
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

abstract class Doodle(val depth: Int, val index: Int) {
    abstract val path: String
}

class NbtDoodle(depth: Int, private val tag: AnyTag, index: Int = -1): Doodle(depth, index) {

    val type = tag.type
    val hasChildren = tag is CompoundTag
            || tag is ListTag
            || tag is ByteArrayTag
            || tag is IntArrayTag
            || tag is LongArrayTag

    val name = tag.name
    val value = if (this.hasChildren) valueSuffix() else tag.valueToString()

    var expanded = false

    private var children: List<Doodle>? = null

    override val path = "${tag.path}"

    fun expand(): List<Doodle> {
        expanded = true
        val newDepth = depth + 1
        val result = when (tag) {
            is CompoundTag -> tag.doodle(newDepth)
            is ListTag -> tag.doodle(newDepth)
            is ByteArrayTag -> tag.doodle(newDepth, path)
            is IntArrayTag -> tag.doodle(newDepth, path)
            is LongArrayTag -> tag.doodle(newDepth, path)
            else -> throw Exception("this tag is not expandable!")
        }
        children = result
        return result
    }

    fun collapse(): Int {
        expanded = false
        val localChildren = children
        val collapsableChildren = if (localChildren == null) 0 else {
            var count = 0
            localChildren.forEach {
                if (it is NbtDoodle && it.hasChildren && it.expanded) count += it.collapse()
            }
            count
        }
        return when (tag) {
            is CompoundTag -> tag.value.size
            is ListTag -> tag.value.size
            is ByteArrayTag -> tag.value.size
            is IntArrayTag -> tag.value.size
            is LongArrayTag -> tag.value.size
            else -> throw Exception("this tag is not collapsable!")
        } + collapsableChildren
    }
    
    private fun valueSuffix(): String {
        return when (tag) {
            is CompoundTag -> "${size(tag.value.size)} child ${tags(tag.value.size)}"
            is ListTag -> "${size(tag.value.size)} ${elementsType(tag.elementsType)}${tags(tag.value.size)} inside"
            is ByteArrayTag -> "${size(tag.value.size)} ${entries(tag.value.size)}"
            is IntArrayTag -> "${size(tag.value.size)} ${entries(tag.value.size)}"
            is LongArrayTag -> "${size(tag.value.size)} ${entries(tag.value.size)}"
            else -> throw Exception("this tag does not have iterable value!")
        }
    }

    private fun size(size: Int): String = if (size > 0) "$size" else "no"

    private fun tags(size: Int): String = if (size != 1) "tags" else "tag"

    private fun entries(size: Int): String = if (size != 1) "entries" else "entry"

    private fun elementsType(type: TagType): String = if (type == TagType.TAG_END) "" else "${type.displayName()} "

}

class PrimitiveValueDoodle(depth: Int, val value: String, override val path: String, index: Int): Doodle(depth, index)
