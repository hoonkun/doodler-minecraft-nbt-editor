package activator.doodler.nbt.tag

import androidx.compose.runtime.Stable
import activator.doodler.nbt.AnyTag
import activator.doodler.nbt.Tag
import activator.doodler.nbt.TagType.*
import java.nio.ByteBuffer

@Stable
class IntArrayTag(
    name: String? = null,
    parent: AnyTag?,
    value: IntArray? = null,
    buffer: ByteBuffer? = null
): Tag<IntArray>(TAG_INT_ARRAY, name, parent, value, buffer) {

    override val sizeInBytes get() = Int.SIZE_BYTES + value.size * Int.SIZE_BYTES

    override fun read(buffer: ByteBuffer, vararg extras: Any?) = IntArray(buffer.int) { buffer.int }

    override fun write(buffer: ByteBuffer) {
        buffer.putInt(value.size)
        value.forEach { buffer.putInt(it) }
    }

    override fun clone(name: String?) = IntArrayTag(name, parent, value = value)

    override fun valueToString(): String = "[ ${value.joinToString(", ")} ]"

    override fun valueEquals(other: AnyTag): Boolean {
        if (javaClass != other.javaClass) return false

        other as IntArrayTag

        return value.contentEquals(other.value)
    }

    override fun valueHashcode(): Int = value.hashCode()

}