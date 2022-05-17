package doodler.nbt.tag

import doodler.nbt.AnyTag
import doodler.nbt.Tag
import doodler.nbt.TagType.*

import java.nio.ByteBuffer

class DoubleTag private constructor(name: String? = null, parent: AnyTag?): Tag<Double>(TAG_DOUBLE, name, parent) {

    override val sizeInBytes get() = Double.SIZE_BYTES

    constructor(value: Double, name: String? = null, parent: AnyTag?): this(name, parent) {
        this.value = value
    }

    constructor(buffer: ByteBuffer, name: String? = null, parent: AnyTag?): this(name, parent) {
        read(buffer)
    }

    override fun read(buffer: ByteBuffer) {
        value = buffer.double
    }

    override fun write(buffer: ByteBuffer) {
        buffer.putDouble(value)
    }

    override fun clone(name: String?) = DoubleTag(value, name, parent)

}