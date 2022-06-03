package activator.doodler.nbt.tag

import androidx.compose.runtime.Stable
import activator.doodler.nbt.AnyTag
import activator.doodler.nbt.Tag
import activator.doodler.nbt.TagType.TAG_FLOAT
import java.nio.ByteBuffer

@Stable
class FloatTag(
    name: String? = null,
    parent: AnyTag?,
    value: Float? = null,
    buffer: ByteBuffer? = null
): Tag<Float>(TAG_FLOAT, name, parent, value, buffer) {

    override val sizeInBytes get() = Float.SIZE_BYTES

    override fun read(buffer: ByteBuffer, vararg extras: Any?) = buffer.float

    override fun write(buffer: ByteBuffer) {
        buffer.putFloat(value)
    }

    override fun clone(name: String?) = FloatTag(name, parent, value = value)

    override fun valueEquals(other: AnyTag): Boolean {
        if (javaClass != other.javaClass) return false

        other as FloatTag

        return value == other.value
    }

    override fun valueHashcode(): Int = value.hashCode()

}