package nbt

import nbt.TagType.*
import nbt.tag.*

import java.nio.ByteBuffer

typealias AnyTag = Tag<out Any>

abstract class Tag<T: Any> protected constructor(
    val type: TagType,
    val name: String?,
    val parent: Tag<out Any>?
) {

    init {
        if (parent != null && parent.type != TAG_COMPOUND && parent.type != TAG_LIST)
            throw Exception("parent must be one of 'TAG_COMPOUND' or 'TAG_LIST'")
    }

    lateinit var value: T

    val path: String? get() = "${
        parent?.path?.plus(
            when (parent.type) {
                TAG_COMPOUND -> if (parent.name == null) "" else "."
                TAG_LIST -> "[${indexInList!!}]."
                else -> ""
            }
        ) ?: ""
    }${name ?: ""}".ifEmpty { null }
    abstract val sizeInBytes: Int

    var indexInList: Int? = null

    inline fun <reified T: AnyTag?> getAs() = this as? T ?: throw Exception("Tag is not a ${T::class.java.simpleName}")

    fun ensureName(name: String?) = if (this.name == name) this else clone(name)

    abstract fun read(buffer: ByteBuffer)

    abstract fun write(buffer: ByteBuffer)

    abstract fun clone(name: String? = this.name): Tag<T>

    open fun prefix() = if (name.isNullOrEmpty()) "" else "\"${name}\": "

    open fun valueToString() = "$value"

    override fun toString(): String = prefix() + valueToString()

    companion object {

        fun read(tagType: TagType, buffer: ByteBuffer, name: String? = null, parent: AnyTag?) = when(tagType) {
            TAG_END -> EndTag()
            TAG_BYTE -> ByteTag(buffer, name, parent)
            TAG_SHORT -> ShortTag(buffer, name, parent)
            TAG_INT -> IntTag(buffer, name, parent)
            TAG_LONG -> LongTag(buffer, name, parent)
            TAG_FLOAT -> FloatTag(buffer, name, parent)
            TAG_DOUBLE -> DoubleTag(buffer, name, parent)
            TAG_BYTE_ARRAY -> ByteArrayTag(buffer, name, parent)
            TAG_STRING -> StringTag(buffer, name, parent)
            TAG_LIST -> ListTag(buffer, name, parent)
            TAG_COMPOUND -> CompoundTag(buffer, name, parent)
            TAG_INT_ARRAY -> IntArrayTag(buffer, name, parent)
            TAG_LONG_ARRAY -> LongArrayTag(buffer, name, parent)
        }

    }

}

enum class TagType(val id: Byte) {
    TAG_END(0),
    TAG_BYTE(1),
    TAG_SHORT(2),
    TAG_INT(3),
    TAG_LONG(4),
    TAG_FLOAT(5),
    TAG_DOUBLE(6),
    TAG_BYTE_ARRAY(7),
    TAG_STRING(8),
    TAG_LIST(9),
    TAG_COMPOUND(10),
    TAG_INT_ARRAY(11),
    TAG_LONG_ARRAY(12);

    companion object {
        private val reversed = values().associateBy { it.id }
        operator fun get(id: Byte): TagType = reversed[id] ?: throw Exception("unknown tag id: $id")
    }
}