package nbt.tag

import nbt.AnyTag
import nbt.Tag
import nbt.TagType.TAG_LONG
import java.nio.ByteBuffer

class LongTag private constructor(name: String? = null, parent: AnyTag?): Tag<Long>(TAG_LONG, name, parent) {

    override val sizeInBytes get() = Long.SIZE_BYTES

    constructor(value: Long, name: String? = null, parent: AnyTag?): this(name, parent) {
        this.value = value
    }

    constructor(buffer: ByteBuffer, name: String? = null, parent: AnyTag?): this(name, parent) {
        read(buffer)
    }

    override fun read(buffer: ByteBuffer) {
        value = buffer.long
    }

    override fun write(buffer: ByteBuffer) {
        buffer.putLong(value)
    }

    override fun clone(name: String?) = LongTag(value, name, parent)

}