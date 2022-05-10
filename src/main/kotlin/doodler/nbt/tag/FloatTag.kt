package nbt.tag

import nbt.AnyTag
import nbt.Tag
import nbt.TagType.TAG_FLOAT
import java.nio.ByteBuffer

class FloatTag private constructor(name: String? = null, parent: AnyTag?): Tag<Float>(TAG_FLOAT, name, parent) {

    override val sizeInBytes get() = Float.SIZE_BYTES

    constructor(value: Float, name: String? = null, parent: AnyTag?): this(name, parent) {
        this.value = value
    }

    constructor(buffer: ByteBuffer, name: String? = null, parent: AnyTag?): this(name, parent) {
        read(buffer)
    }

    override fun read(buffer: ByteBuffer) {
        value = buffer.float
    }

    override fun write(buffer: ByteBuffer) {
        buffer.putFloat(value)
    }

    override fun clone(name: String?) = FloatTag(value, name, parent)

}