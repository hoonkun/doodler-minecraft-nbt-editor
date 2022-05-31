package doodler.doodle

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import doodler.doodle.extensions.displayName
import doodler.doodle.extensions.doodle
import doodler.extensions.replaceAt
import doodler.nbt.AnyTag
import doodler.nbt.TagType
import doodler.nbt.tag.*


@Stable
sealed class Doodle(
    depth: Int,
    index: Int
){
    var depth by mutableStateOf(depth)
    var index by mutableStateOf(index)

    abstract val path: String
}

@Stable
abstract class VirtualDoodle(
    depth: Int,
    index: Int,
    val parent: NbtDoodle,
): Doodle(depth, index) {
    override val path: String = "_DOODLE_CREATOR_"

    fun primitiveActualize(rawName: String, type: TagType, value: String): AnyTag {
        val parentTag = parent.tag
        val name = rawName.ifEmpty { null }

        return when (type) {
            TagType.TAG_BYTE -> ByteTag(name, parentTag, value = value.toByte())
            TagType.TAG_SHORT -> ShortTag(name, parentTag, value = value.toShort())
            TagType.TAG_INT -> IntTag(name, parentTag, value = value.toInt())
            TagType.TAG_LONG -> LongTag(name, parentTag, value = value.toLong())
            TagType.TAG_FLOAT -> FloatTag(name, parentTag, value = value.toFloat())
            TagType.TAG_DOUBLE -> DoubleTag(name, parentTag, value = value.toDouble())
            TagType.TAG_STRING -> StringTag(name, parentTag, value = value)
            else -> throw DoodleException("Internal Error", null, "Non-Primitive Tag Creation is not handled by this function!")
        }
    }

    abstract fun actualize(rawName: String, value: String, intoIndex: Int): ActualDoodle

}

@Stable
class NbtCreationDoodle(
    val type: TagType,
    depth: Int,
    index: Int,
    parent: NbtDoodle,
): VirtualDoodle(depth, index, parent) {

    override fun actualize(rawName: String, value: String, intoIndex: Int): ActualDoodle {
        val parentTag = parent.tag
        val name = rawName.ifEmpty { null }

        if (type.isPrimitive())
            return NbtDoodle(primitiveActualize(rawName, type, value), depth, intoIndex, parent)

        val tag = when (type) {
            TagType.TAG_BYTE_ARRAY -> ByteArrayTag(name, parentTag, value = ByteArray(0))
            TagType.TAG_INT_ARRAY -> IntArrayTag(name, parentTag, value = IntArray(0))
            TagType.TAG_LONG_ARRAY -> LongArrayTag(name, parentTag, value = LongArray(0))
            TagType.TAG_LIST -> ListTag(name, parentTag, TagType.TAG_END, value = mutableStateListOf())
            TagType.TAG_COMPOUND -> CompoundTag(name, parentTag, value = mutableStateListOf())
            TagType.TAG_END -> throw EndCreationException()
            else -> throw DoodleException("Internal Error", null, "Primitive Tag Creation is not handled by this function.")
        }
        return NbtDoodle(tag, depth, intoIndex, parent)
    }

}

@Stable
class ValueCreationDoodle(
    depth: Int,
    index: Int,
    parent: NbtDoodle,
): VirtualDoodle(depth, index, parent) {

    override fun actualize(rawName: String, value: String, intoIndex: Int): ActualDoodle =
        ValueDoodle(value, depth, intoIndex, parent)

}

@Stable
class NbtEditionDoodle(
    val from: NbtDoodle,
    depth: Int,
    index: Int,
    parent: NbtDoodle
): VirtualDoodle(depth, index, parent) {

    override fun actualize(rawName: String, value: String, intoIndex: Int): ActualDoodle {
        val parentTag = parent.tag
        val name = rawName.ifEmpty { null }

        val type = from.tag.type

        if (type.isPrimitive())
            return NbtDoodle(primitiveActualize(rawName, type, value), depth, intoIndex, parent)

        val tag = when (type) {
            TagType.TAG_BYTE_ARRAY -> ByteArrayTag(name, parentTag, value = from.tag.getAs<ByteArrayTag>().value)
            TagType.TAG_INT_ARRAY -> IntArrayTag(name, parentTag, value = from.tag.getAs<IntArrayTag>().value)
            TagType.TAG_LONG_ARRAY -> LongArrayTag(name, parentTag, value = from.tag.getAs<LongArrayTag>().value)
            TagType.TAG_LIST -> ListTag(name, parentTag, TagType.TAG_END, value = from.tag.getAs<ListTag>().value)
            TagType.TAG_COMPOUND -> CompoundTag(name, parentTag, value = from.tag.getAs<CompoundTag>().value)
            TagType.TAG_END -> throw EndCreationException()
            else -> throw DoodleException("Internal Error", null, "Primitive Tag Creation is not handled by this function.")
        }
        return NbtDoodle(tag, depth, intoIndex, parent)
    }

}

@Stable
class ValueEditionDoodle(
    depth: Int,
    index: Int,
    parent: NbtDoodle,
): VirtualDoodle(depth, index, parent) {

    override fun actualize(rawName: String, value: String, intoIndex: Int): ActualDoodle =
        ValueDoodle(value, depth, intoIndex, parent)

}


sealed class ActualDoodle(
    depth: Int,
    index: Int,
    var parent: NbtDoodle?
): Doodle(depth, index) {
    abstract fun delete(): ActualDoodle?

    abstract fun clone(parent: NbtDoodle?): ActualDoodle

    fun checkNameConflict(): Pair<Boolean, Int> {
        if (this !is NbtDoodle) return Pair(false, -1)

        val into = this.parent ?: return Pair(false, -1)
        if (into.tag.type.isCompound()) {
            val tag = into.tag.getAs<CompoundTag>()
            val conflictTag = tag.value.find { it.name == this.name }
            if (conflictTag != null)
                return Pair(true, tag.value.indexOf(conflictTag))
        }
        return Pair(false, -1)
    }
}

class NbtDoodle (
    val tag: AnyTag,
    depth: Int,
    index: Int = -1,
    parent: NbtDoodle? = null
): ActualDoodle(depth, index, parent) {

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

    var creator: VirtualDoodle? by mutableStateOf(null)
    val expandedItems: SnapshotStateList<ActualDoodle> = mutableStateListOf()
    val collapsedItems: MutableList<ActualDoodle> = mutableListOf()

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

    fun sizeOfChildren(root: Boolean = false): Int {
        var size = 0
        if (!root) size++
        size += expandedItems.sumOf { if (it is NbtDoodle) it.sizeOfChildren() else 1 }
        size += if (creator is NbtCreationDoodle || creator is ValueCreationDoodle) 1 else 0
        return size
    }

    fun children(root: Boolean = false): List<Doodle> {
        return mutableListOf<Doodle>().apply {
            if (!root) add(this@NbtDoodle)
            addAll(expandedItems.map { if (it is NbtDoodle) it.children() else listOf(it) }.flatten())
            creator?.let {
                when (it) {
                    is NbtCreationDoodle, is ValueCreationDoodle -> add(it.index + if (!root) 1 else 0, it)
                    is NbtEditionDoodle, is ValueEditionDoodle -> replaceAt(it.index + if (!root) 1 else 0, it)
                }
            }
        }
    }

    private fun initializeChildren() {
        if (collapsedItems.isNotEmpty() || expandedItems.isNotEmpty()) return

        val newDepth = depth + 1
        collapsedItems.addAll(
            when (tag) {
                is CompoundTag -> tag.doodle(this, newDepth)
                is ListTag -> tag.doodle(this, newDepth)
                is ByteArrayTag -> tag.doodle(this, newDepth)
                is IntArrayTag -> tag.doodle(this, newDepth)
                is LongArrayTag -> tag.doodle(this, newDepth)
                else -> throw ChildrenInitiationException(tag.type)
            }
        )
    }

    fun expand() {
        if (!tag.canHaveChildren) return
        if (expanded) return

        parent?.let { if (!it.expanded) it.expand() }

        expanded = true

        initializeChildren()

        expandedItems.addAll(collapsedItems)
        collapsedItems.clear()
    }

    fun collapse(selected: MutableList<ActualDoodle>) {
        if (!tag.canHaveChildren) return
        if (!expanded) return

        expanded = false

        expandedItems.forEach {
            selected.remove(it)
            if (it is NbtDoodle && it.tag.canHaveChildren && it.expanded) it.collapse(selected)
        }

        collapsedItems.addAll(expandedItems)
        expandedItems.clear()
    }

    override fun delete(): NbtDoodle? {
        val parent = parent ?: return null

        when (parent.tag.type) {
            TagType.TAG_COMPOUND -> parent.tag.getAs<CompoundTag>().remove(tag.name)
            TagType.TAG_LIST -> {
                val parentTag = parent.tag.getAs<ListTag>()

                parentTag.value.remove(tag)
                if (parentTag.value.isEmpty()) parentTag.elementsType = TagType.TAG_END
            }
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

    fun create(new: ActualDoodle, useIndex: Boolean = true): ActualDoodle {
        initializeChildren()

        new.depth = depth + 1
        if (new is NbtDoodle) {
            new.expandedItems.forEach { it.depth = new.depth + 1 }
            new.collapsedItems.forEach { it.depth = new.depth + 1 }
        }

        when (tag.type) {
            TagType.TAG_COMPOUND -> {
                new as? NbtDoodle ?: throw InternalAssertionException(NbtDoodle::class.java.simpleName, new.javaClass.name)

                if (useIndex) tag.getAs<CompoundTag>().insert(new.index, new.tag)
                else tag.getAs<CompoundTag>().add(new.tag)

                if (!useIndex) new.index = tag.getAs<CompoundTag>().value.size - 1
            }
            TagType.TAG_LIST -> {
                new as? NbtDoodle ?: throw InternalAssertionException(NbtDoodle::class.java.simpleName, new.javaClass.name)

                val list = tag.getAs<ListTag>()

                if (list.elementsType != new.tag.type && list.elementsType != TagType.TAG_END)
                    throw ListElementsTypeMismatchException(list.elementsType, new.tag.type)
                else {
                    if (useIndex) list.value.add(new.index, new.tag)
                    else list.value.add(new.tag)

                    if (list.elementsType == TagType.TAG_END) list.elementsType = new.tag.type
                }

                if (!useIndex) new.index = list.value.size - 1
            }
            TagType.TAG_BYTE_ARRAY -> {
                new as? ValueDoodle ?: throw InternalAssertionException(ValueDoodle::class.java.simpleName, new.javaClass.name)

                val value = new.value.toByteOrNull() ?: throw ValueTypeMismatchException("Byte", new.value)

                val array = tag.getAs<ByteArrayTag>()
                array.value = array.value.toMutableList().apply {
                    if (useIndex) add(new.index, value)
                    else add(value)
                }.toByteArray()

                if (!useIndex) new.index = array.value.size - 1
            }
            TagType.TAG_INT_ARRAY -> {
                new as? ValueDoodle ?: throw InternalAssertionException(ValueDoodle::class.java.simpleName, new.javaClass.name)

                val value = new.value.toIntOrNull() ?: throw ValueTypeMismatchException("Int", new.value)

                val array = tag.getAs<IntArrayTag>()
                array.value = array.value.toMutableList().apply {
                    if (useIndex) add(new.index, value)
                    else add(value)
                }.toIntArray()

                if (!useIndex) new.index = array.value.size - 1
            }
            TagType.TAG_LONG_ARRAY -> {
                new as? ValueDoodle ?: throw InternalAssertionException(ValueDoodle::class.java.simpleName, new.javaClass.name)

                val value = new.value.toLongOrNull() ?: throw ValueTypeMismatchException("Long", new.value)

                val array = tag.getAs<LongArrayTag>()
                array.value = array.value.toMutableList().apply {
                    if (useIndex) add(new.index, value)
                    else add(value)
                }.toLongArray()

                if (!useIndex) new.index = array.value.size - 1
            }
            else -> throw InvalidCreationException(tag.type)
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
                else -> throw ValueSuffixException(tag.type)
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
): ActualDoodle(depth, index, parent) {

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