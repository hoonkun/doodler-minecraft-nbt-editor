package nbt.tag

import nbt.AnyTag
import nbt.Tag
import nbt.TagType.*
import java.nio.ByteBuffer

class ByteArrayTag private constructor(name: String? = null, parent: AnyTag?): Tag<ByteArray>(TAG_BYTE_ARRAY, name, parent) {

    override val sizeInBytes get() = Int.SIZE_BYTES + value.size

    constructor(value: ByteArray, name: String? = null, parent: AnyTag?): this(name, parent) {
        this.value = value
    }

    constructor(buffer: ByteBuffer, name: String? = null, parent: AnyTag?): this(name, parent) {
        read(buffer)
    }

    override fun read(buffer: ByteBuffer) {
        val length = buffer.int

        value = ByteArray(length)
        buffer.get(value, 0, length)
    }

    override fun write(buffer: ByteBuffer) {
        buffer.putInt(value.size)
        buffer.put(value)
    }

    override fun clone(name: String?) = ByteArrayTag(value, name, parent)

    override fun valueToString(): String = "[ ${value.joinToString(", ")} ]"

}