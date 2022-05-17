package doodler.nbt.tag

import doodler.nbt.AnyTag
import doodler.nbt.Tag
import doodler.nbt.TagType.TAG_SHORT
import java.nio.ByteBuffer

class ShortTag private constructor(name: String? = null, parent: AnyTag?): Tag<Short>(TAG_SHORT, name, parent) {

    override val sizeInBytes get() = Short.SIZE_BYTES

    constructor(value: Short, name: String? = null, parent: AnyTag?): this(name, parent) {
        this.value = value
    }

    constructor(buffer: ByteBuffer, name: String? = null, parent: AnyTag?): this(name, parent) {
        read(buffer)
    }

    override fun read(buffer: ByteBuffer) {
        value = buffer.short
    }

    override fun write(buffer: ByteBuffer) {
        buffer.putShort(value)
    }

    override fun clone(name: String?) = ShortTag(value, name, parent)

}