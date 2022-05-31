package doodler.nbt.tag

import androidx.compose.runtime.Stable
import doodler.nbt.AnyTag
import doodler.nbt.Tag
import doodler.nbt.TagType.TAG_INT
import java.nio.ByteBuffer

@Stable
class IntTag(
    name: String? = null,
    parent: AnyTag?,
    value: Int? = null,
    buffer: ByteBuffer? = null
): Tag<Int>(TAG_INT, name, parent, value, buffer) {

    override val sizeInBytes get() = Int.SIZE_BYTES

    override fun read(buffer: ByteBuffer, vararg extras: Any?) = buffer.int

    override fun write(buffer: ByteBuffer) {
        buffer.putInt(value)
    }

    override fun clone(name: String?) = IntTag(name, parent, value = value)

}