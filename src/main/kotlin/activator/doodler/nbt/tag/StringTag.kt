package activator.doodler.nbt.tag

import androidx.compose.runtime.Stable
import activator.doodler.nbt.AnyTag
import activator.doodler.nbt.Tag
import activator.doodler.nbt.TagType.TAG_STRING
import activator.doodler.nbt.extensions.putString
import activator.doodler.nbt.extensions.string
import java.nio.ByteBuffer

@Stable
class StringTag(
    name: String? = null,
    parent: AnyTag?,
    value: String? = null,
    buffer: ByteBuffer? = null
): Tag<String>(TAG_STRING, name, parent, value, buffer) {

    override val sizeInBytes get() = Short.SIZE_BYTES * value.toByteArray().size

    override fun read(buffer: ByteBuffer, vararg extras: Any?) = buffer.string

    override fun write(buffer: ByteBuffer) {
        buffer.putString(value)
    }

    override fun clone(name: String?): Tag<String> = StringTag(name, parent, value = value)

    override fun valueToString(): String = "\"$value\""

    override fun valueEquals(other: AnyTag): Boolean {
        if (javaClass != other.javaClass) return false

        other as StringTag

        return value == other.value
    }

    override fun valueHashcode(): Int = value.hashCode()

}