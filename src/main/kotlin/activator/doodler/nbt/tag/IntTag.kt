package activator.doodler.nbt.tag

import androidx.compose.runtime.Stable
import activator.doodler.nbt.AnyTag
import activator.doodler.nbt.Tag
import activator.doodler.nbt.TagType.TAG_INT
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


    override fun valueEquals(other: AnyTag): Boolean {
        if (javaClass != other.javaClass) return false

        other as IntTag

        return value == other.value
    }

    override fun valueHashcode(): Int = value.hashCode()

}