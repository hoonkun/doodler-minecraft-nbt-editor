package activator.doodler.nbt.tag

import androidx.compose.runtime.Stable
import activator.doodler.nbt.AnyTag
import activator.doodler.nbt.Tag
import activator.doodler.nbt.TagType.*
import java.nio.ByteBuffer

@Stable
class ByteArrayTag(
    name: String? = null,
    parent: AnyTag?,
    value: ByteArray? = null,
    buffer: ByteBuffer? = null
): Tag<ByteArray>(TAG_BYTE_ARRAY, name, parent, value, buffer) {

    override val sizeInBytes get() = Int.SIZE_BYTES + value.size

    override fun read(buffer: ByteBuffer, vararg extras: Any?): ByteArray {
        val length = buffer.int

        val newValue = ByteArray(length)
        buffer.get(newValue, 0, length)
        return newValue
    }

    override fun write(buffer: ByteBuffer) {
        buffer.putInt(value.size)
        buffer.put(value)
    }

    override fun clone(name: String?) = ByteArrayTag(name, parent, value = value)

    override fun valueToString(): String = "[ ${value.joinToString(", ")} ]"

    override fun valueEquals(other: AnyTag): Boolean {
        if (javaClass != other.javaClass) return false

        other as ByteArrayTag

        return value.contentEquals(other.value)
    }

    override fun valueHashcode(): Int = value.contentHashCode()

}