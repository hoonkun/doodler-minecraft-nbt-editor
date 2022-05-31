package doodler.nbt.tag

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import doodler.nbt.AnyTag
import doodler.nbt.Tag
import doodler.nbt.TagType.*
import java.nio.ByteBuffer

@Stable
class ByteArrayTag private constructor(name: String? = null, parent: AnyTag?): Tag<ByteArray>(TAG_BYTE_ARRAY, name, parent) {

    override val sizeInBytes get() = Int.SIZE_BYTES + value.size

    constructor(value: ByteArray, name: String? = null, parent: AnyTag?): this(name, parent) {
        valueState = mutableStateOf(value)
    }

    constructor(buffer: ByteBuffer, name: String? = null, parent: AnyTag?): this(name, parent) {
        read(buffer)
    }

    override fun read(buffer: ByteBuffer) {
        val length = buffer.int

        val newValue = ByteArray(length)
        buffer.get(newValue, 0, length)
        valueState = mutableStateOf(newValue)
    }

    override fun write(buffer: ByteBuffer) {
        buffer.putInt(value.size)
        buffer.put(value)
    }

    override fun clone(name: String?) = ByteArrayTag(value, name, parent)

    override fun valueToString(): String = "[ ${value.joinToString(", ")} ]"

}