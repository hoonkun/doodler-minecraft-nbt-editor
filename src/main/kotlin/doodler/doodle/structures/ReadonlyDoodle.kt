package doodler.doodle.structures

import doodler.doodle.extensions.displayName
import doodler.doodle.extensions.doodle
import activator.doodler.doodle.*
import activator.doodler.nbt.AnyTag
import activator.doodler.nbt.TagType
import activator.doodler.nbt.tag.*
import androidx.compose.runtime.*


@Stable
sealed class ReadonlyDoodle(
    depth: Int,
    parent: TagDoodle?
): Doodle(depth) {

    var parent by mutableStateOf(parent)

    abstract val index: Int

    abstract fun delete()

    abstract fun clone(parent: TagDoodle?, depth: Int = this.depth): ReadonlyDoodle

}

@Stable
class TagDoodle(
    val tag: AnyTag,
    depth: Int,
    parent: TagDoodle?
): ReadonlyDoodle(depth, parent) {

    override val path: String get() =
        parent?.let {
            when (it.tag.type) {
                TagType.TAG_COMPOUND -> "${it.path}.${tag.name}"
                TagType.TAG_LIST -> "${it.path}[${it.tag.getAs<ListTag>().value.indexOf(tag)}]"
                else -> "" // no-op
            }
        } ?: "root"

    var expanded by mutableStateOf(false)

    var action by mutableStateOf<ActionDoodle?>(null)

    val children = mutableListOf<ReadonlyDoodle>()

    val items: List<Doodle> by derivedStateOf {
        mutableListOf<Doodle>().apply {
            parent?.action?.let { if (it is EditorDoodle && it.source.path == path) add(it) else add(this@TagDoodle) }

            if (!expanded) return@apply

            action?.let { if (it is CreatorDoodle) add(it) }
            addAll(children.map { if (it is TagDoodle) it.items else listOf(it) }.flatten())
        }
    }

    val value: String get() = if (tag.type.canHaveChildren()) valueOfExpandable(tag) else "${tag.value}"

    override val index: Int
        get() {
            val parent = parent ?: return 0

            return when (parent.tag.type) {
                TagType.TAG_LIST -> parent.tag.getAs<ListTag>().value.indexOf(tag)
                TagType.TAG_COMPOUND -> parent.tag.getAs<CompoundTag>().value.indexOf(tag)
                else -> 0
            }
        }

    fun expand() {
        if (!tag.type.canHaveChildren()) return
        if (expanded) return

        parent?.let { if (!it.expanded) it.expand() }

        initChildrenIfEmpty()

        expanded = true
    }

    fun collapse(): List<ReadonlyDoodle> {
        if (!tag.type.canHaveChildren()) return emptyList()
        if (!expanded) return emptyList()

        expanded = false

        val collapsed = mutableListOf<ReadonlyDoodle>()

        collapsed.addAll(children)

        children.filterIsInstance<TagDoodle>()
            .filter { it.tag.type.canHaveChildren() && it.expanded }
            .forEach { collapsed.addAll(it.collapse()) }

        return collapsed
    }

    fun hasConflict(): ConflictInfo? {
        val into = this.parent ?: return null
        if (into.tag.type.isCompound()) {
            val intoTag = into.tag.getAs<CompoundTag>()
            intoTag.value.find { it.name == this.tag.name }
                .let { if (it != null) return ConflictInfo(this.tag.name, intoTag.value.indexOf(it)) }
        }
        return null
    }

    private fun initChildrenIfEmpty() {
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

    fun createChild(new: ReadonlyDoodle, index: Int? = null): ReadonlyDoodle {
        val assertionAsTag = tag.type.canHaveChildren() && !tag.type.isArray() && new !is TagDoodle
        val assertionAsValue = tag.type.isArray() && new !is ArrayValueDoodle

        if (assertionAsTag || assertionAsValue)
            throw InternalAssertionException(
                (if (!assertionAsTag) TagDoodle::class else ArrayValueDoodle::class).java.simpleName,
                new.javaClass.name
            )

        initChildrenIfEmpty()

        when (tag.type) {
            TagType.TAG_COMPOUND -> {
                val doodle = new as TagDoodle
                val tag = tag.getAs<CompoundTag>()

                if (index != null) tag.insert(index, doodle.tag)
                else tag.add(doodle.tag)
            }
            TagType.TAG_LIST -> {
                val doodle = new as TagDoodle
                val tag = tag.getAs<ListTag>()

                if (tag.elementsType != new.tag.type && tag.elementsType != TagType.TAG_END)
                    throw ListElementsTypeMismatchException(tag.elementsType, new.tag.type)

                if (index != null) tag.value.add(index, doodle.tag)
                else tag.value.add(doodle.tag)

                if (tag.elementsType == TagType.TAG_END) tag.elementsType = doodle.tag.type
            }
            TagType.TAG_BYTE_ARRAY -> {
                val doodle = new as ArrayValueDoodle
                val value = doodle.value.toByteOrNull() ?: throw ValueTypeMismatchException("Byte", new.value)

                tag.getAs<ByteArrayTag>().let {
                    it.value = it.value
                        .toMutableList()
                        .apply {
                            if (index != null) add(index, value)
                            else add(value)
                        }
                        .toByteArray()
                }
            }
            TagType.TAG_INT_ARRAY -> {
                val doodle = new as ArrayValueDoodle
                val value = doodle.value.toIntOrNull() ?: throw ValueTypeMismatchException("Int", new.value)

                tag.getAs<IntArrayTag>().let {
                    it.value = it.value
                        .toMutableList()
                        .apply {
                            if (index != null) add(index, value)
                            else add(value)
                        }
                        .toIntArray()
                }
            }
            TagType.TAG_LONG_ARRAY -> {
                val doodle = new as ArrayValueDoodle
                val value = doodle.value.toLongOrNull() ?: throw ValueTypeMismatchException("Long", new.value)

                tag.getAs<LongArrayTag>().let {
                    it.value = it.value
                        .toMutableList()
                        .apply {
                            if (index != null) add(index, value)
                            else add(value)
                        }
                        .toLongArray()
                }
            }
            else -> throw InvalidCreationException(tag.type)
        }

        if (index != null) children.add(index, new)
        else children.add(new)

        return new
    }

    override fun delete() {
        val parent = parent ?: return

        when (parent.tag.type) {
            TagType.TAG_COMPOUND -> parent.tag.getAs<CompoundTag>().remove(tag.name)
            TagType.TAG_LIST -> {
                parent.tag.getAs<ListTag>().let {
                    it.value.remove(tag)
                    if (it.value.isEmpty()) it.elementsType = TagType.TAG_END
                }
            }
            else -> { /* no-op */ }
        }

        parent.children.remove(this)
    }

    override fun clone(parent: TagDoodle?, depth: Int): TagDoodle =
        TagDoodle(tag.clone(tag.name), depth, parent)
            .apply {
                expanded = this@TagDoodle.expanded
                children.addAll(this@TagDoodle.children.map { it.clone(this, depth + 1) })
            }

    data class ConflictInfo(
        val name: String?,
        val where: Int
    )

    companion object {
        
        private fun valueOfExpandable(tag: AnyTag): String {
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
class ArrayValueDoodle(
    val value: String,
    depth: Int,
    parent: TagDoodle?
): ReadonlyDoodle(depth, parent) {

    override val path: String
        get() = parent?.let {
            when (it.tag.type) {
                TagType.TAG_BYTE_ARRAY,
                TagType.TAG_INT_ARRAY,
                TagType.TAG_LONG_ARRAY -> "${it.path}[${index}]"
                else -> "" // no-op
            }
        } ?: "" // no-op

    override val index: Int
        get() {
            val parent = parent ?: return 0
            return when (parent.tag.type) {
                TagType.TAG_BYTE_ARRAY, TagType.TAG_INT_ARRAY, TagType.TAG_LONG_ARRAY -> parent.children.indexOf(this)
                else -> 0 // no-op
            }
        }

    override fun clone(parent: TagDoodle?, depth: Int): ArrayValueDoodle =
        ArrayValueDoodle(value, depth, parent)

    override fun delete() {
        val parent = parent ?: return

        when (parent.tag.type) {
            TagType.TAG_BYTE_ARRAY -> {
                parent.tag.getAs<ByteArrayTag>().let {
                    it.value = it.value.toMutableList().apply { removeAt(index) }.toByteArray()
                }
            }
            TagType.TAG_INT_ARRAY -> {
                parent.tag.getAs<IntArrayTag>().let {
                    it.value = it.value.toMutableList().apply { removeAt(index) }.toIntArray()
                }
            }
            TagType.TAG_LONG_ARRAY -> {
                parent.tag.getAs<LongArrayTag>().let {
                    it.value = it.value.toMutableList().apply { removeAt(index) }.toLongArray()
                }
            }
            else -> { /* no-op */ }
        }

        parent.children.remove(this)
    }

}
