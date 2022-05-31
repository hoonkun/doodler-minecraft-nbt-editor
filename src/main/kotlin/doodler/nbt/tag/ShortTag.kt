package doodler.nbt.tag

import androidx.compose.runtime.Stable
import doodler.nbt.AnyTag
import doodler.nbt.Tag
import doodler.nbt.TagType.TAG_SHORT
import java.nio.ByteBuffer

@Stable
class ShortTag(
    name: String? = null,
    parent: AnyTag?,
    value: Short? = null,
    buffer: ByteBuffer? = null
): Tag<Short>(TAG_SHORT, name, parent, value, buffer) {

    override val sizeInBytes get() = Short.SIZE_BYTES

    override fun read(buffer: ByteBuffer, vararg extras: Any?) = buffer.short

    override fun write(buffer: ByteBuffer) {
        buffer.putShort(value)
    }

    override fun clone(name: String?) = ShortTag(name, parent, value = value)

}