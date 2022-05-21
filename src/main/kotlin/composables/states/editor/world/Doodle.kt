package composables.states.editor.world

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import composables.states.editor.world.extensions.displayName
import composables.states.editor.world.extensions.doodle
import composables.states.editor.world.extensions.replaceAt
import doodler.nbt.AnyTag
import doodler.nbt.TagType
import doodler.nbt.tag.*


sealed class Doodle(
    var depth: Int,
    var index: Int,
) {
    abstract val path: String
}

abstract class VirtualDoodle(
    depth: Int,
    index: Int,
    val parent: NbtDoodle,
    val mode: VirtualMode
): Doodle(depth, index) {
    override val path: String = "_DOODLE_CREATOR_"

    lateinit var from: ActualDoodle
    
    fun actualize(rawName: String, value: String, intoIndex: Int): ActualDoodle {
        val name = rawName.ifEmpty { null }
        val parentTag = parent.tag
        return if (this is NbtCreationDoodle) {
            val tag = when (type) {
                TagType.TAG_BYTE -> ByteTag(value.toByte(), name, parentTag)
                TagType.TAG_SHORT -> ShortTag(value.toShort(), name, parentTag)
                TagType.TAG_INT -> IntTag(value.toInt(), name, parentTag)
                TagType.TAG_LONG -> LongTag(value.toLong(), name, parentTag)
                TagType.TAG_FLOAT -> FloatTag(value.toFloat(), name, parentTag)
                TagType.TAG_DOUBLE -> DoubleTag(value.toDouble(), name, parentTag)
                TagType.TAG_STRING -> StringTag(value, name, parentTag)
                TagType.TAG_BYTE_ARRAY -> ByteArrayTag(
                    if (mode.isEdit()) (from as NbtDoodle).tag.getAs<ByteArrayTag>().value else ByteArray(0),
                    name, parentTag
                )
                TagType.TAG_INT_ARRAY -> IntArrayTag(
                    if (mode.isEdit()) (from as NbtDoodle).tag.getAs<IntArrayTag>().value else IntArray(0),
                    name, parentTag
                )
                TagType.TAG_LONG_ARRAY -> LongArrayTag(
                    if (mode.isEdit()) (from as NbtDoodle).tag.getAs<LongArrayTag>().value else LongArray(0),
                    name, parentTag
                )
                TagType.TAG_LIST -> ListTag(
                    TagType.TAG_END,
                    if (mode.isEdit()) (from as NbtDoodle).tag.getAs<ListTag>().value else listOf(),
                    true, name, parentTag
                )
                TagType.TAG_COMPOUND -> CompoundTag(
                    if (mode.isEdit()) (from as NbtDoodle).tag.getAs<CompoundTag>().value else mutableListOf(),
                    name, parentTag
                )
                TagType.TAG_END -> throw EndCreationException()
            }
            NbtDoodle(tag, depth, intoIndex, parent)
        } else {
            ValueDoodle(value, depth, intoIndex, parent)
        }
    }

    enum class VirtualMode {
        CREATE, EDIT;

        fun isEdit() = this == EDIT
    }
}

class NbtCreationDoodle(
    val type: TagType,
    depth: Int,
    index: Int,
    parent: NbtDoodle,
    mode: VirtualMode
): VirtualDoodle(depth, index, parent, mode) {
    constructor(from: NbtDoodle, mode: VirtualMode):
            this(from.tag.type, from.depth, from.index, from.parent!!, mode) {
                this.from = from
            }
}

class ValueCreationDoodle(
    depth: Int,
    index: Int,
    parent: NbtDoodle,
    mode: VirtualMode
): VirtualDoodle(depth, index, parent, mode) {
    constructor(from: ValueDoodle, mode: VirtualMode):
            this(from.depth, from.index, from.parent!!, mode) {
                this.from = from
            }
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

    fun children(root: Boolean = false): List<Doodle> {
        return mutableListOf<Doodle>().apply {
            if (!root) add(this@NbtDoodle)
            addAll(expandedItems.map { if (it is NbtDoodle) it.children() else listOf(it) }.flatten())
            creator?.let {
                when (it.mode) {
                    VirtualDoodle.VirtualMode.CREATE -> add(it.index + 1, it)
                    VirtualDoodle.VirtualMode.EDIT -> replaceAt(it.index + 1, it)
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