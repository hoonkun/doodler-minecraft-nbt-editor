package doodler.nbt.tag

import androidx.compose.runtime.Stable
import doodler.nbt.AnyTag
import doodler.nbt.Tag
import doodler.nbt.TagType.*

import java.nio.ByteBuffer

@Stable
class DoubleTag(
    name: String? = null,
    parent: AnyTag?,
    value: Double? = null,
    buffer: ByteBuffer? = null
): Tag<Double>(TAG_DOUBLE, name, parent, value, buffer) {

    override val sizeInBytes get() = Double.SIZE_BYTES

    override fun read(buffer: ByteBuffer, vararg extras: Any?) = buffer.double

    override fun write(buffer: ByteBuffer) {
        buffer.putDouble(value)
    }

    override fun clone(name: String?) = DoubleTag(name, parent, value = value)

    override fun valueEquals(other: AnyTag): Boolean {
        if (javaClass != other.javaClass) return false

        other as DoubleTag

        return value == other.value
    }

    override fun valueHashcode(): Int = value.hashCode()

}