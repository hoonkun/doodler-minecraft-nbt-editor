package doodler.doodle

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import nbt.AnyTag
import nbt.TagType
import nbt.tag.*

fun CompoundTag.doodle(parent: NbtDoodle?, depth: Int): List<Doodle> {
    return this.value.values.map { NbtDoodle(it, depth, parentTag = parent) }
}

fun ListTag.doodle(parent: NbtDoodle, depth: Int): List<Doodle> {
    return this.value.mapIndexed { index, value -> NbtDoodle(value, depth, index, parent) }
}

fun ByteArrayTag.doodle(parent: NbtDoodle, depth: Int, parentKey: String): List<Doodle> {
    return this.value.mapIndexed { index, value ->
        PrimitiveValueDoodle("$value", depth, "$parentKey[$index]", index, parent)
    }
}

fun IntArrayTag.doodle(parent: NbtDoodle, depth: Int, parentKey: String): List<Doodle> {
    return this.value.mapIndexed { index, value ->
        PrimitiveValueDoodle("$value", depth, "$parentKey[$index]", index, parent)
    }
}

fun LongArrayTag.doodle(parent: NbtDoodle, depth: Int, parentKey: String): List<Doodle> {
    return this.value.mapIndexed { index, value ->
        PrimitiveValueDoodle("$value", depth, "$parentKey[$index]", index, parent)
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

abstract class Doodle(val depth: Int, val index: Int, val parentTag: NbtDoodle?) {
    abstract val path: String
}

class NbtDoodle(
    val tag: AnyTag,
    depth: Int,
    index: Int = -1,
    parentTag: NbtDoodle? = null
): Doodle(depth, index, parentTag) {

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
            is CompoundTag -> tag.doodle(this, newDepth)
            is ListTag -> tag.doodle(this, newDepth)
            is ByteArrayTag -> tag.doodle(this, newDepth, path)
            is IntArrayTag -> tag.doodle(this, newDepth, path)
            is LongArrayTag -> tag.doodle(this, newDepth, path)
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

class PrimitiveValueDoodle(
    val value: String,
    depth: Int,
    override val path: String,
    index: Int,
    parentTag: NbtDoodle? = null
): Doodle(depth, index, parentTag)

@Composable
fun rememberDoodleState(
    selected: SnapshotStateList<Doodle?> = remember { mutableStateListOf() },
    pressed: MutableState<Doodle?> = remember { mutableStateOf(null) },
    focusedDirectly: MutableState<Doodle?> = remember { mutableStateOf(null) },
    focusedTree: MutableState<Doodle?> = remember { mutableStateOf(null) },
    focusedTreeView: MutableState<Doodle?> = remember { mutableStateOf(null) }
) = remember(selected, pressed, focusedDirectly, focusedTree, focusedTreeView) {
    DoodleState(selected, pressed, focusedDirectly, focusedTree, focusedTreeView)
}

class DoodleState(
    val selected: SnapshotStateList<Doodle?>,
    pressed: MutableState<Doodle?>,
    focusedDirectly: MutableState<Doodle?>,
    focusedTree: MutableState<Doodle?>,
    focusedTreeView: MutableState<Doodle?>
) {
    var pressed by pressed
    var focusedDirectly by focusedDirectly
    var focusedTree by focusedTree
    var focusedTreeView by focusedTreeView

    fun press(target: Doodle) {
        pressed = target
    }

    fun unPress(target: Doodle) {
        if (pressed == target) pressed = null
    }

    fun getLastSelected(): Doodle? = if (selected.isEmpty()) null else selected.last()

    fun addRangeToSelected(targets: List<Doodle>) {
        selected.addAll(targets.filter { !selected.contains(it) })
    }

    fun addToSelected(target: Doodle) {
        if (!selected.contains(target)) selected.add(target)
    }

    fun removeFromSelected(target: Doodle) {
        if (selected.contains(target)) selected.remove(target)
    }

    fun setSelected(target: Doodle) {
        selected.clear()
        selected.add(target)
    }

    fun focusDirectly(target: Doodle) {
        if (focusedDirectly != target) focusedDirectly = target
    }

    fun unFocusDirectly(target: Doodle) {
        if (focusedDirectly == target) focusedDirectly = null
    }

    fun focusTreeView(target: Doodle) {
        if (focusedTreeView != target) focusedTreeView = target
    }

    fun unFocusTreeView(target: Doodle) {
        if (focusedTreeView == target) focusedTreeView = null
    }

    fun focusTree(target: Doodle) {
        if (focusedTree != target) focusedTree = target
    }

    fun unFocusTree(target: Doodle) {
        if (focusedTree == target) focusedTree = null
    }

}