package doodler.nbt

import androidx.compose.runtime.*
import doodler.exceptions.DoodleException
import doodler.exceptions.InternalAssertionException
import doodler.nbt.tag.*
import doodler.nbt.TagType.*
import doodler.nbt.extensions.byte

import java.nio.ByteBuffer

typealias AnyTag = Tag<out Any>

@Stable
abstract class Tag<T: Any>(
    val type: TagType,
    val name: String?,
    val parent: Tag<out Any>?,
    valueInput: T? = null,
    buffer: ByteBuffer? = null,
    vararg extras: Any?
) {

    init {
        if (parent != null && parent.type != TAG_COMPOUND && parent.type != TAG_LIST)
            throw Exception("parent must be one of 'TAG_COMPOUND' or 'TAG_LIST'")
    }

    private val valueState: MutableState<T> =
        mutableStateOf(
            valueInput ?: buffer?.let { read(it, *extras) } ?: throw DoodleException("Internal Error", null, "Not enough arguments.")
        )
    var value by valueState

    abstract val sizeInBytes: Int

    val canHaveChildren = type.canHaveChildren()

    inline fun <reified T: AnyTag?> getAs() = this as? T ?: throw Exception("Tag is not a ${T::class.java.simpleName}")

    fun ensureName(name: String?) = if (this.name == name) this else clone(name)

    abstract fun read(buffer: ByteBuffer, vararg extras: Any?): T

    abstract fun write(buffer: ByteBuffer)

    abstract fun clone(name: String? = this.name): Tag<T>

    abstract fun valueEquals(other: AnyTag): Boolean

    // TODO: 이거 SnapshotStateList.hashCode() 로 괜찮은가?
    abstract fun valueHashcode(): Int

    open fun prefix() = if (name.isNullOrEmpty()) "" else "\"${name}\": "

    open fun valueToString() = "$value"

    override fun toString(): String = prefix() + valueToString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnyTag

        if (name != other.name) return false
        if (!valueEquals(other)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = result * 31 + valueHashcode()
        return result
    }

    fun canHold(other: TagType) =
        this.type == TAG_COMPOUND ||
        (this.type == TAG_LIST && (this.getAs<ListTag>().let { it.elementsType == other || it.elementsType == TAG_END })) ||
        (this.type == TAG_BYTE_ARRAY && other == TAG_BYTE) ||
        (this.type == TAG_INT_ARRAY && other == TAG_INT) ||
        (this.type == TAG_LONG_ARRAY && other == TAG_LONG)


    companion object {

        fun read(tagType: TagType, buffer: ByteBuffer, name: String? = null, parent: AnyTag?) = when(tagType) {
            TAG_END -> EndTag()
            TAG_BYTE -> ByteTag(name, parent, buffer = buffer)
            TAG_SHORT -> ShortTag(name, parent, buffer = buffer)
            TAG_INT -> IntTag(name, parent, buffer = buffer)
            TAG_LONG -> LongTag(name, parent, buffer = buffer)
            TAG_FLOAT -> FloatTag(name, parent, buffer = buffer)
            TAG_DOUBLE -> DoubleTag(name, parent, buffer = buffer)
            TAG_BYTE_ARRAY -> ByteArrayTag(name, parent, buffer = buffer)
            TAG_STRING -> StringTag(name, parent, buffer = buffer)
            TAG_LIST -> ListTag(name, parent, TagType[buffer.byte], buffer = buffer)
            TAG_COMPOUND -> CompoundTag(name, parent, buffer = buffer)
            TAG_INT_ARRAY -> IntArrayTag(name, parent, buffer = buffer)
            TAG_LONG_ARRAY -> LongArrayTag(name, parent, buffer = buffer)
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

    fun isArray() =
        this == TAG_BYTE_ARRAY ||
        this == TAG_INT_ARRAY ||
        this == TAG_LONG_ARRAY

    fun isNumber() =
        this == TAG_BYTE ||
        this == TAG_INT ||
        this == TAG_SHORT ||
        this == TAG_LONG ||
        this == TAG_FLOAT ||
        this == TAG_DOUBLE

    fun isString() = this == TAG_STRING

    fun isList() = this == TAG_LIST

    fun isCompound() = this == TAG_COMPOUND

    fun isEnd() = this == TAG_END

    fun canHaveChildren() =
        this == TAG_BYTE_ARRAY ||
        this == TAG_INT_ARRAY ||
        this == TAG_LONG_ARRAY ||
        this == TAG_LIST ||
        this == TAG_COMPOUND

    fun isPrimitive() = !canHaveChildren()

    fun arrayElementType(): TagType = when (this) {
        TAG_BYTE_ARRAY -> TAG_BYTE
        TAG_INT_ARRAY -> TAG_INT
        TAG_LONG_ARRAY -> TAG_LONG
        else -> throw InternalAssertionException("Array Tag", "$this")
    }

    companion object {
        private val reversed = values().associateBy { it.id }
        operator fun get(id: Byte): TagType = reversed[id] ?: throw Exception("unknown tag id: $id")
    }
}