package doodler.nbt.tag

import androidx.compose.runtime.Stable
import doodler.nbt.AnyTag
import doodler.nbt.Tag
import doodler.nbt.TagType.*
import java.nio.ByteBuffer

@Stable
class LongArrayTag(
    name: String? = null,
    parent: AnyTag?,
    value: LongArray? = null,
    buffer: ByteBuffer? = null
): Tag<LongArray>(TAG_LONG_ARRAY, name, parent, value, buffer) {

    override val sizeInBytes get() = Long.SIZE_BYTES + value.size * Long.SIZE_BYTES

    override fun read(buffer: ByteBuffer, vararg extras: Any?) = LongArray(buffer.int) { buffer.long }

    override fun write(buffer: ByteBuffer) {
        buffer.putInt(value.size)
        value.forEach { buffer.putLong(it) }
    }

    override fun clone(name: String?) = LongArrayTag(name, parent, value = value)

    override fun valueToString(): String = "[ ${value.joinToString(", ")} ]"

    override fun valueEquals(other: AnyTag): Boolean {
        if (javaClass != other.javaClass) return false

        other as LongArrayTag

        return value.contentEquals(other.value)
    }

    override fun valueHashcode(): Int = value.hashCode()

}