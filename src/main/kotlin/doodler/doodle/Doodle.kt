package doodler.doodle

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import doodler.doodle.extensions.displayName
import doodler.doodle.extensions.doodle
import doodler.nbt.AnyTag
import doodler.nbt.TagType
import doodler.nbt.tag.*


@Stable
sealed class Doodle(
    val depth: Int,
){
    abstract val path: String
}

@Stable
abstract class VirtualDoodle(
    depth: Int,
    val parent: NbtDoodle,
): Doodle(depth) {
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

    abstract fun actualize(rawName: String, value: String): ActualDoodle

}

@Stable
sealed class CreationDoodle(
    depth: Int,
    parent: NbtDoodle
): VirtualDoodle(depth, parent)

@Stable
class NbtCreationDoodle(
    val type: TagType,
    depth: Int,
    parent: NbtDoodle,
): CreationDoodle(depth, parent) {

    override fun actualize(rawName: String, value: String): ActualDoodle {
        val parentTag = parent.tag
        val name = rawName.ifEmpty { null }

        if (type.isPrimitive())
            return NbtDoodle(primitiveActualize(rawName, type, value), depth, parent)

        val tag = when (type) {
            TagType.TAG_BYTE_ARRAY -> ByteArrayTag(name, parentTag, value = ByteArray(0))
            TagType.TAG_INT_ARRAY -> IntArrayTag(name, parentTag, value = IntArray(0))
            TagType.TAG_LONG_ARRAY -> LongArrayTag(name, parentTag, value = LongArray(0))
            TagType.TAG_LIST -> ListTag(name, parentTag, TagType.TAG_END, value = mutableStateListOf())
            TagType.TAG_COMPOUND -> CompoundTag(name, parentTag, value = mutableStateListOf())
            TagType.TAG_END -> throw EndCreationException()
            else -> throw DoodleException("Internal Error", null, "Primitive Tag Creation is not handled by this function.")
        }
        return NbtDoodle(tag, depth, parent)
    }

}

@Stable
class ValueCreationDoodle(
    depth: Int,
    parent: NbtDoodle,
): CreationDoodle(depth, parent) {

    override fun actualize(rawName: String, value: String): ActualDoodle =
        ValueDoodle(value, depth, parent)

}

@Stable
sealed class EditionDoodle(
    open val from: ActualDoodle,
    depth: Int,
    parent: NbtDoodle
): VirtualDoodle(depth, parent)

@Stable
class NbtEditionDoodle(
    override val from: NbtDoodle
): EditionDoodle(from, from.depth, from.parent!!) {

    override fun actualize(rawName: String, value: String): ActualDoodle {
        val parentTag = parent.tag
        val name = rawName.ifEmpty { null }

        val type = from.tag.type

        if (type.isPrimitive())
            return NbtDoodle(primitiveActualize(rawName, type, value), depth, parent)

        val tag = when (type) {
            TagType.TAG_BYTE_ARRAY -> ByteArrayTag(name, parentTag, value = from.tag.getAs<ByteArrayTag>().value)
            TagType.TAG_INT_ARRAY -> IntArrayTag(name, parentTag, value = from.tag.getAs<IntArrayTag>().value)
            TagType.TAG_LONG_ARRAY -> LongArrayTag(name, parentTag, value = from.tag.getAs<LongArrayTag>().value)
            TagType.TAG_LIST -> ListTag(name, parentTag, TagType.TAG_END, value = from.tag.getAs<ListTag>().value)
            TagType.TAG_COMPOUND -> CompoundTag(name, parentTag, value = from.tag.getAs<CompoundTag>().value)
            TagType.TAG_END -> throw EndCreationException()
            else -> throw DoodleException("Internal Error", null, "Primitive Tag Creation is not handled by this function.")
        }
        return NbtDoodle(tag, depth, parent)
    }

}

@Stable
class ValueEditionDoodle(
    override val from: ValueDoodle,
): EditionDoodle(from, from.depth, from.parent!!) {

    override fun actualize(rawName: String, value: String): ActualDoodle =
        ValueDoodle(value, depth, parent)

}


@Stable
sealed class ActualDoodle(
    depth: Int,
    parent: NbtDoodle?
): Doodle(depth) {

    var parent by mutableStateOf(parent)

    abstract fun index(): Int

    abstract fun delete(): ActualDoodle?

    abstract fun clone(parent: NbtDoodle?, newDepth: Int = depth): ActualDoodle

    abstract fun cloneAsChild(parent: NbtDoodle): ActualDoodle

    fun checkNameConflict(): Pair<Boolean, Int> {
        if (this !is NbtDoodle) return Pair(false, -1)

        val into = this.parent ?: return Pair(false, -1)
        if (into.tag.type.isCompound()) {
            val tag = into.tag.getAs<CompoundTag>()
            val conflictTag = tag.value.find { it.name == this.tag.name }
            if (conflictTag != null)
                return Pair(true, tag.value.indexOf(conflictTag))
        }
        return Pair(false, -1)
    }
}

@Stable
class NbtDoodle (
    val tag: AnyTag,
    depth: Int,
    parent: NbtDoodle? = null,
): ActualDoodle(depth, parent) {

    override val path: String get() = parent?.let {
        when (it.tag.type) {
            TagType.TAG_COMPOUND -> "${it.path}.${tag.name}"
            TagType.TAG_LIST -> "${it.path}[${it.tag.getAs<ListTag>().value.indexOf(tag)}]"
            else -> "" // no-op
        }
    } ?: "root"

    private val root = parent == null

    var expanded by mutableStateOf(false)

    var virtual by mutableStateOf<VirtualDoodle?>(null)
    val children = mutableStateListOf<ActualDoodle>()

    val doodles: SnapshotStateList<Doodle> by derivedStateOf {
        if (!expanded) mutableStateListOf<Doodle>().apply { if (!root) add(this@NbtDoodle) }
        else
            mutableStateListOf<Doodle>().apply {
                if (!root) parent!!.virtual.let { if (it is EditionDoodle && it.from.path == path) add(it) else add(this@NbtDoodle) }
                virtual?.let { if (it is CreationDoodle) add(it) }
                addAll(children.map { if (it is NbtDoodle) it.doodles else mutableStateListOf(it) }.flatten())
            }
    }

    fun value(): String = if (tag.canHaveChildren) valueSuffix(tag) else tag.value.toString()

    override fun index(): Int {
        val parent = parent ?: return 0

        return when (parent.tag.type) {
            TagType.TAG_LIST -> parent.tag.getAs<ListTag>().value.indexOf(tag)
            TagType.TAG_COMPOUND -> parent.tag.getAs<CompoundTag>().value.indexOf(tag)
            else -> 0
        }
    }

    private fun initializeChildren() {
        if (children.isNotEmpty()) return

        val newDepth = depth + 1
        children.addAll(
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

        initializeChildren()

        expanded = true
    }

    fun collapse(selected: MutableList<ActualDoodle>) {
        if (!tag.canHaveChildren) return
        if (!expanded) return

        expanded = false

        children.forEach {
            selected.remove(it)
            if (it is NbtDoodle && it.tag.canHaveChildren && it.expanded) it.collapse(selected)
        }
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

        parent.children.remove(this)

        return this
    }

    override fun clone(parent: NbtDoodle?, newDepth: Int): NbtDoodle {
        return NbtDoodle(tag.clone(tag.name), newDepth, parent)
            .apply {
                expanded = this@NbtDoodle.expanded
                children.addAll(this@NbtDoodle.children.map { it.clone(this, newDepth + 1) })
            }
    }

    override fun cloneAsChild(parent: NbtDoodle): ActualDoodle {
        return clone(parent, depth + 1)
    }

    fun create(new: ActualDoodle, index: Int? = null): ActualDoodle {
        initializeChildren()

        when (tag.type) {
            TagType.TAG_COMPOUND -> {
                new as? NbtDoodle ?: throw InternalAssertionException(NbtDoodle::class.java.simpleName, new.javaClass.name)

                if (index != null) tag.getAs<CompoundTag>().insert(index, new.tag)
                else tag.getAs<CompoundTag>().add(new.tag)
            }
            TagType.TAG_LIST -> {
                new as? NbtDoodle ?: throw InternalAssertionException(NbtDoodle::class.java.simpleName, new.javaClass.name)

                val list = tag.getAs<ListTag>()

                if (list.elementsType != new.tag.type && list.elementsType != TagType.TAG_END)
                    throw ListElementsTypeMismatchException(list.elementsType, new.tag.type)
                else {
                    if (index != null) list.value.add(index, new.tag)
                    else list.value.add(new.tag)

                    if (list.elementsType == TagType.TAG_END) list.elementsType = new.tag.type
                }
            }
            TagType.TAG_BYTE_ARRAY -> {
                new as? ValueDoodle ?: throw InternalAssertionException(ValueDoodle::class.java.simpleName, new.javaClass.name)

                val value = new.value.toByteOrNull() ?: throw ValueTypeMismatchException("Byte", new.value)

                val array = tag.getAs<ByteArrayTag>()
                array.value = array.value.toMutableList().apply {
                    if (index != null) add(index, value)
                    else add(value)
                }.toByteArray()
            }
            TagType.TAG_INT_ARRAY -> {
                new as? ValueDoodle ?: throw InternalAssertionException(ValueDoodle::class.java.simpleName, new.javaClass.name)

                val value = new.value.toIntOrNull() ?: throw ValueTypeMismatchException("Int", new.value)

                val array = tag.getAs<IntArrayTag>()
                array.value = array.value.toMutableList().apply {
                    if (index != null) add(index, value)
                    else add(value)
                }.toIntArray()
            }
            TagType.TAG_LONG_ARRAY -> {
                new as? ValueDoodle ?: throw InternalAssertionException(ValueDoodle::class.java.simpleName, new.javaClass.name)

                val value = new.value.toLongOrNull() ?: throw ValueTypeMismatchException("Long", new.value)

                val array = tag.getAs<LongArrayTag>()
                array.value = array.value.toMutableList().apply {
                    if (index != null) add(index, value)
                    else add(value)
                }.toLongArray()
            }
            else -> throw InvalidCreationException(tag.type)
        }

        if (index == null) children.add(new)
        else children.add(index, new)

        return new
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

@Stable
class ValueDoodle (
    val value: String,
    depth: Int,
    parent: NbtDoodle?
): ActualDoodle(depth, parent) {

    override val path: String
        get() = parent?.let {
            when (it.tag.type) {
                TagType.TAG_BYTE_ARRAY,
                TagType.TAG_INT_ARRAY,
                TagType.TAG_LONG_ARRAY -> "${it.path}[${index()}]"
                else -> "" // no-op
            }
        } ?: "" // no-op

    override fun index(): Int {
        val parent = parent ?: return 0
        return when (parent.tag.type) {
            TagType.TAG_BYTE_ARRAY -> parent.tag.getAs<ByteArrayTag>().value.indexOf(value.toByte())
            TagType.TAG_INT_ARRAY -> parent.tag.getAs<IntArrayTag>().value.indexOf(value.toInt())
            TagType.TAG_LONG_ARRAY -> parent.tag.getAs<LongArrayTag>().value.indexOf(value.toLong())
            else -> 0
        }
    }

    override fun cloneAsChild(parent: NbtDoodle): ActualDoodle =
        clone(parent, depth + 1)

    override fun delete(): ValueDoodle? {
        val parent = parent ?: return null

        val index = index()

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

        parent.children.remove(this)

        return this
    }

    override fun clone(parent: NbtDoodle?, newDepth: Int): ValueDoodle {
        return ValueDoodle(value, newDepth, parent)
    }

}