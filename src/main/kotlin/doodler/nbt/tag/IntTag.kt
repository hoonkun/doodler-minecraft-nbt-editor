package doodler.nbt.tag

import doodler.nbt.AnyTag
import doodler.nbt.Tag
import doodler.nbt.TagType.TAG_INT
import java.nio.ByteBuffer

class IntTag private constructor(name: String? = null, parent: AnyTag?): Tag<Int>(TAG_INT, name, parent) {

    override val sizeInBytes get() = Int.SIZE_BYTES

    constructor(value: Int, name: String? = null, parent: AnyTag?): this(name, parent) {
        this.value = value
    }

    constructor(buffer: ByteBuffer, name: String? = null, parent: AnyTag?): this(name, parent) {
        read(buffer)
    }

    override fun read(buffer: ByteBuffer) {
        value = buffer.int
    }

    override fun write(buffer: ByteBuffer) {
        buffer.putInt(value)
    }

    override fun clone(name: String?) = IntTag(value, name, parent)

}