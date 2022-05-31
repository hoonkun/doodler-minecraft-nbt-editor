package doodler.nbt.tag

import androidx.compose.runtime.Stable
import doodler.nbt.AnyTag
import doodler.nbt.Tag
import doodler.nbt.TagType.*
import doodler.nbt.extensions.byte

import java.nio.ByteBuffer

@Stable
class ByteTag(
    name: String? = null,
    parent: AnyTag?,
    value: Byte? = null,
    buffer: ByteBuffer? = null
): Tag<Byte>(TAG_BYTE, name, parent, value, buffer) {

    override val sizeInBytes get() = Byte.SIZE_BYTES

    override fun read(buffer: ByteBuffer, vararg extras: Any?) = buffer.byte

    override fun write(buffer: ByteBuffer) {
        buffer.put(value)
    }

    override fun clone(name: String?) = ByteTag(name, parent, value = value)

}