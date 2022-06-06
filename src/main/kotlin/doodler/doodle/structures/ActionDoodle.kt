package doodler.doodle.structures

import doodler.exceptions.EndCreationException
import doodler.nbt.AnyTag
import doodler.nbt.TagType
import doodler.nbt.tag.*
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf


@Stable
sealed class ActionDoodle(
    val parent: TagDoodle,
    depth: Int,
): Doodle(depth) {

    override val path: String = "_DOODLE_ACTION_"

    private inline fun <reified T: AnyTag>createWithFactory(
        creationTagFactory: TagCreatorDoodle.() -> T,
        editionName: String?
    ): T {
        return when (this) {
            is TagCreatorDoodle -> creationTagFactory()
            is TagEditorDoodle -> this.source.tag.clone(editionName) as T
            else -> throw Exception()
        }
    }

    fun commitTag(type: TagType, name: String, value: String): ReadonlyDoodle {
        if (this !is TagCreatorDoodle && this !is TagEditorDoodle) throw Exception()

        val parentTag = parent.tag
        val tagName = name.ifEmpty { null }

        val newTag = when (type) {
            TagType.TAG_END -> throw EndCreationException()
            TagType.TAG_BYTE -> ByteTag(tagName, parentTag, value = value.toByte())
            TagType.TAG_SHORT -> ShortTag(tagName, parentTag, value = value.toShort())
            TagType.TAG_INT -> IntTag(tagName, parentTag, value = value.toInt())
            TagType.TAG_LONG -> LongTag(tagName, parentTag, value = value.toLong())
            TagType.TAG_FLOAT -> FloatTag(tagName, parentTag, value = value.toFloat())
            TagType.TAG_DOUBLE -> DoubleTag(tagName, parentTag, value = value.toDouble())
            TagType.TAG_STRING -> StringTag(tagName, parentTag, value = value)
            TagType.TAG_COMPOUND -> createWithFactory(
                creationTagFactory = { CompoundTag(tagName, parentTag, value = mutableStateListOf()) },
                editionName = tagName
            )
            TagType.TAG_LIST -> createWithFactory(
                creationTagFactory = { ListTag(tagName, parentTag, TagType.TAG_END, value = mutableStateListOf()) },
                editionName = tagName
            )
            TagType.TAG_BYTE_ARRAY -> createWithFactory(
                creationTagFactory = { ByteArrayTag(tagName, parentTag, ByteArray(0)) },
                editionName = tagName
            )
            TagType.TAG_INT_ARRAY -> createWithFactory(
                creationTagFactory = { IntArrayTag(tagName, parentTag, IntArray(0)) },
                editionName = tagName
            )
            TagType.TAG_LONG_ARRAY -> createWithFactory(
                creationTagFactory = { LongArrayTag(tagName, parentTag, LongArray(0)) },
                editionName = tagName
            )
        }

        return TagDoodle(newTag, depth, parent)
    }

    fun commitValue(value: String): ReadonlyDoodle = ArrayValueDoodle(value, depth, parent)

}

@Stable
sealed class CreatorDoodle(
    parent: TagDoodle,
    depth: Int
): ActionDoodle(parent, depth)

@Stable
sealed class EditorDoodle(
    open val source: ReadonlyDoodle,
    parent: TagDoodle,
    depth: Int
): ActionDoodle(parent, depth)

@Stable
class TagCreatorDoodle(
    val type: TagType,
    parent: TagDoodle
): CreatorDoodle(parent, parent.depth + 1)

@Stable
class ArrayValueCreatorDoodle(
    parent: TagDoodle
): CreatorDoodle(parent, parent.depth + 1)

@Stable
class TagEditorDoodle(
    override val source: TagDoodle
): EditorDoodle(source, source.parent!!, source.depth)

@Stable
class ArrayValueEditorDoodle(
    override val source: ArrayValueDoodle
): EditorDoodle(source, source.parent!!, source.depth)