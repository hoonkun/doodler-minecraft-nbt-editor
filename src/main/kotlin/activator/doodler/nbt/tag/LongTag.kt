package activator.doodler.nbt.tag

import androidx.compose.runtime.Stable
import activator.doodler.nbt.AnyTag
import activator.doodler.nbt.Tag
import activator.doodler.nbt.TagType.TAG_LONG
import java.nio.ByteBuffer

@Stable
class LongTag(
    name: String? = null,
    parent: AnyTag?,
    value: Long? = null,
    buffer: ByteBuffer? = null
): Tag<Long>(TAG_LONG, name, parent, value, buffer) {

    override val sizeInBytes get() = Long.SIZE_BYTES

    override fun read(buffer: ByteBuffer, vararg extras: Any?) = buffer.long

    override fun write(buffer: ByteBuffer) {
        buffer.putLong(value)
    }

    override fun clone(name: String?) = LongTag(name, parent, value = value)

    override fun valueEquals(other: AnyTag): Boolean {
        if (javaClass != other.javaClass) return false

        other as LongTag

        return value == other.value
    }

    override fun valueHashcode(): Int = value.hashCode()

}