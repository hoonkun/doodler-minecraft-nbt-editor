package nbt.tag

import nbt.AnyTag
import nbt.Tag
import nbt.TagType.TAG_STRING
import nbt.extensions.putString
import nbt.extensions.string
import java.nio.ByteBuffer

class StringTag private constructor(name: String? = null, parent: AnyTag?): Tag<String>(TAG_STRING, name, parent) {

    override val sizeInBytes get() = Short.SIZE_BYTES * value.toByteArray().size

    constructor(value: String, name: String? = null, parent: AnyTag?): this(name, parent) {
        this.value = value
    }

    constructor(buffer: ByteBuffer, name: String? = null, parent: AnyTag?): this(name, parent) {
        read(buffer)
    }

    override fun read(buffer: ByteBuffer) {
        value = buffer.string
    }

    override fun write(buffer: ByteBuffer) {
        buffer.putString(value)
    }

    override fun clone(name: String?): Tag<String> = StringTag(value, name, parent)

    override fun valueToString(): String = "\"$value\""

}