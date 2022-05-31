package doodler.nbt.tag

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import doodler.nbt.AnyTag
import doodler.nbt.Tag
import doodler.nbt.TagType.TAG_SHORT
import java.nio.ByteBuffer

@Stable
class ShortTag private constructor(name: String? = null, parent: AnyTag?): Tag<Short>(TAG_SHORT, name, parent) {

    override val sizeInBytes get() = Short.SIZE_BYTES

    constructor(value: Short, name: String? = null, parent: AnyTag?): this(name, parent) {
        valueState = mutableStateOf(value)
    }

    constructor(buffer: ByteBuffer, name: String? = null, parent: AnyTag?): this(name, parent) {
        read(buffer)
    }

    override fun read(buffer: ByteBuffer) {
        valueState = mutableStateOf(buffer.short)
    }

    override fun write(buffer: ByteBuffer) {
        buffer.putShort(value)
    }

    override fun clone(name: String?) = ShortTag(value, name, parent)

}