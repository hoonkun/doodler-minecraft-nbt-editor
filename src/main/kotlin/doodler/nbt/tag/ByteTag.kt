package doodler.nbt.tag

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import doodler.nbt.AnyTag
import doodler.nbt.Tag
import doodler.nbt.TagType.*
import doodler.nbt.extensions.byte

import java.nio.ByteBuffer

@Stable
class ByteTag private constructor(name: String? = null, parent: AnyTag?): Tag<Byte>(TAG_BYTE, name, parent) {

    override val sizeInBytes get() = Byte.SIZE_BYTES

    constructor(value: Byte, name: String? = null, parent: AnyTag?): this(name, parent) {
        valueState = mutableStateOf(value)
    }

    constructor(buffer: ByteBuffer, name: String? = null, parent: AnyTag?): this(name, parent) {
        read(buffer)
    }

    override fun read(buffer: ByteBuffer) {
        valueState = mutableStateOf(buffer.byte)
    }

    override fun write(buffer: ByteBuffer) {
        buffer.put(value)
    }

    override fun clone(name: String?) = ByteTag(value, name, parent)

}