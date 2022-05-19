package composables.states.editor.world

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import composables.states.editor.world.extensions.displayName
import composables.states.editor.world.extensions.doodle
import doodler.nbt.AnyTag
import doodler.nbt.TagType
import doodler.nbt.tag.*


sealed class Doodle (
    var depth: Int,
    var index: Int,
    var parent: NbtDoodle?
) {
    abstract val path: String

    abstract fun delete(): Doodle?

    abstract fun clone(parent: NbtDoodle?): Doodle
}

class NbtDoodle (
    val tag: AnyTag,
    depth: Int,
    index: Int = -1,
    parent: NbtDoodle? = null
): Doodle(depth, index, parent) {

    override val path: String get() = parent?.let {
        when (it.tag.type) {
            TagType.TAG_COMPOUND -> "${it.path}.${tag.name}"
            TagType.TAG_LIST -> "${it.path}[${index}]"
            else -> "" // no-op
        }
    } ?: "root"

    var name by mutableStateOf(tag.name)
        private set
    var value by mutableStateOf(if (this.tag.canHaveChildren) valueSuffix(tag) else tag.valueToString())
        private set

    var expanded = false

    val expandedItems: SnapshotStateList<Doodle> = mutableStateListOf()
    val collapsedItems: MutableList<Doodle> = mutableListOf()

    fun update(vararg targets: UpdateTarget) {
        if (targets.contains(UpdateTarget.NAME))
            name = tag.name
        if (targets.contains(UpdateTarget.VALUE))
            value = if (this.tag.canHaveChildren) valueSuffix(tag) else tag.valueToString()
        if (targets.contains(UpdateTarget.INDEX)) {
            if (expanded) {
                expandedItems.forEach { it.index = expandedItems.indexOf(it) }
            } else {
                collapsedItems.forEach { it.index = collapsedItems.indexOf(it) }
            }
        }
    }

    fun children(root: Boolean = false): List<Doodle> {
        return mutableListOf<Doodle>().apply {
            if (!root) add(this@NbtDoodle)
            addAll(expandedItems.map { if (it is NbtDoodle) it.children() else listOf(it) }.flatten())
        }
    }

    private fun initialDoodles(depth: Int): List<Doodle> {
        return when (tag) {
            is CompoundTag -> tag.doodle(this, depth)
            is ListTag -> tag.doodle(this, depth)
            is ByteArrayTag -> tag.doodle(this, depth)
            is IntArrayTag -> tag.doodle(this, depth)
            is LongArrayTag -> tag.doodle(this, depth)
            else -> throw Exception("this tag is not expandable!")
        }
    }

    fun expand() {
        if (!tag.canHaveChildren) return
        if (expanded) return

        parent?.let { if (!it.expanded) it.expand() }

        expanded = true

        val newDepth = depth + 1

        if (collapsedItems.isEmpty() && expandedItems.isEmpty()) {
            expandedItems.addAll(initialDoodles(newDepth))
        } else {
            expandedItems.addAll(collapsedItems)
            collapsedItems.clear()
        }
    }

    fun collapse() {
        if (!tag.canHaveChildren) return
        if (!expanded) return

        expanded = false

        expandedItems.forEach {
            if (it is NbtDoodle && it.tag.canHaveChildren && it.expanded) it.collapse()
        }

        collapsedItems.addAll(expandedItems)
        expandedItems.clear()
    }

    override fun delete(): NbtDoodle? {
        val parent = parent ?: return null

        when (parent.tag.type) {
            TagType.TAG_COMPOUND -> parent.tag.getAs<CompoundTag>().remove(tag.name)
            TagType.TAG_LIST -> parent.tag.getAs<ListTag>().value.remove(tag)
            else -> { /* no-op */ }
        }

        parent.expandedItems.remove(this)
        parent.collapsedItems.remove(this)

        return this
    }

    override fun clone(parent: NbtDoodle?): NbtDoodle {
        return NbtDoodle(tag.clone(tag.name), depth, index, parent)
            .apply {
                expanded = this@NbtDoodle.expanded
                expandedItems.addAll(this@NbtDoodle.expandedItems.map { it.clone(this) })
                collapsedItems.addAll(this@NbtDoodle.collapsedItems.map { it.clone(this) })
            }
    }

    fun create(new: Doodle, useIndex: Boolean = true): Doodle {
        new.depth = depth + 1

        when (tag.type) {
            TagType.TAG_COMPOUND -> {
                new as? NbtDoodle
                    ?: throw Exception("invalid operation: internal error. expected: NbtDoodle, actual was: ${new.javaClass.name}")

                if (useIndex) tag.getAs<CompoundTag>().insert(new.index, new.tag)
                else tag.getAs<CompoundTag>().add(new.tag)

                if (!useIndex) new.index = tag.getAs<CompoundTag>().value.size - 1
            }
            TagType.TAG_LIST -> {
                new as? NbtDoodle
                    ?: throw Exception("invalid operation: internal error. expected: NbtDoodle, actual was: ${new.javaClass.name}")

                val list = tag.getAs<ListTag>()

                if (list.elementsType != new.tag.type)
                    throw Exception("invalid operation: tag type mismatch. expected: ${tag.type.name}, actual was: ${new.tag.type.name}")
                else {
                    if (useIndex) list.value.add(new.index, new.tag)
                    else list.value.add(new.tag)

                    if (list.elementsType == TagType.TAG_END) list.elementsType = new.tag.type
                }

                if (!useIndex) new.index = list.value.size - 1
            }
            TagType.TAG_BYTE_ARRAY -> {
                new as? ValueDoodle
                    ?: throw Exception("invalid operation: internal error. expected: ValueDoodle, actual was: ${new.javaClass.name}")

                val value = new.value.toByteOrNull()
                    ?: throw Exception("invalid operation: value type mismatch. expected: Byte, actual was: ${new.value}")

                val array = tag.getAs<ByteArrayTag>()
                array.value = array.value.toMutableList().apply {
                    if (useIndex) add(new.index, value)
                    else add(value)
                }.toByteArray()

                if (!useIndex) new.index = array.value.size - 1
            }
            TagType.TAG_INT_ARRAY -> {
                new as? ValueDoodle
                    ?: throw Exception("invalid operation: internal error. expected: ValueDoodle, actual was: ${new.javaClass.name}")

                val value = new.value.toIntOrNull()
                    ?: throw Exception("invalid operation: value type mismatch. expected: Byte, actual was: ${new.value}")

                val array = tag.getAs<IntArrayTag>()
                array.value = array.value.toMutableList().apply {
                    if (useIndex) add(new.index, value)
                    else add(value)
                }.toIntArray()

                if (!useIndex) new.index = array.value.size - 1
            }
            TagType.TAG_LONG_ARRAY -> {
                new as? ValueDoodle
                    ?: throw Exception("invalid operation: internal error. expected: ValueDoodle, actual was: ${new.javaClass.name}")

                val value = new.value.toLongOrNull()
                    ?: throw Exception("invalid operation: value type mismatch. expected: Byte, actual was: ${new.value}")

                val array = tag.getAs<LongArrayTag>()
                array.value = array.value.toMutableList().apply {
                    if (useIndex) add(new.index, value)
                    else add(value)
                }.toLongArray()

                if (!useIndex) new.index = array.value.size - 1
            }
            else -> throw Exception("invalid operation: ${tag.javaClass.name} cannot own child tags.")
        }

        if (expanded) {
            if (new.index == -1 || !useIndex) expandedItems.add(new)
            else expandedItems.add(new.index, new)
        } else {
            if (new.index == -1 || !useIndex) collapsedItems.add(new)
            else collapsedItems.add(new.index, new)
        }

        return new
    }

    enum class UpdateTarget {
        NAME, VALUE, INDEX
    }

    companion object {

        private fun valueSuffix(tag: AnyTag): String {
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

}

class ValueDoodle (
    val value: String,
    depth: Int,
    index: Int,
    parent: NbtDoodle?
): Doodle(depth, index, parent) {

    override val path: String
        get() = parent?.let {
            when (it.tag.type) {
                TagType.TAG_BYTE_ARRAY,
                TagType.TAG_INT_ARRAY,
                TagType.TAG_LONG_ARRAY -> "${it.path}[${index}]"
                else -> "" // no-op
            }
        } ?: "" // no-op

    override fun delete(): ValueDoodle? {
        val parent = parent ?: return null

        when (parent.tag.type) {
            TagType.TAG_BYTE_ARRAY -> {
                val tag = parent.tag.getAs<ByteArrayTag>()
                tag.value = tag.value.toMutableList().apply { removeAt(index) }.toByteArray()
            }
            TagType.TAG_INT_ARRAY -> {
                val tag = parent.tag.getAs<IntArrayTag>()
                tag.value = tag.value.toMutableList().apply { removeAt(index) }.toIntArray()
            }
            TagType.TAG_LONG_ARRAY -> {
                val tag = parent.tag.getAs<LongArrayTag>()
                tag.value = tag.value.toMutableList().apply { removeAt(index) }.toLongArray()
            }
            else -> { /* no-op */ }
        }

        parent.expandedItems.remove(this)
        parent.collapsedItems.remove(this)

        return this
    }

    override fun clone(parent: NbtDoodle?): ValueDoodle {
        return ValueDoodle(value, depth, index, parent)
    }

}