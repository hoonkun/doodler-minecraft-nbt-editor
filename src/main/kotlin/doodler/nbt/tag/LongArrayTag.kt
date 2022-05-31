package doodler.nbt.tag

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import doodler.nbt.AnyTag
import doodler.nbt.Tag
import doodler.nbt.TagType.*
import java.nio.ByteBuffer

@Stable
class LongArrayTag private constructor(name: String? = null, parent: AnyTag?): Tag<LongArray>(TAG_LONG_ARRAY, name, parent) {

    override val sizeInBytes get() = Long.SIZE_BYTES + value.size * Long.SIZE_BYTES

    constructor(value: LongArray, name: String? = null, parent: AnyTag?): this(name, parent) {
        valueState = mutableStateOf(value)
    }

    constructor(buffer: ByteBuffer, name: String? = null, parent: AnyTag?): this(name, parent) {
        read(buffer)
    }

    override fun read(buffer: ByteBuffer) {
        valueState = mutableStateOf(LongArray(buffer.int) { buffer.long })
    }

    override fun write(buffer: ByteBuffer) {
        buffer.putInt(value.size)
        value.forEach { buffer.putLong(it) }
    }

    override fun clone(name: String?) = LongArrayTag(value, name, parent)

    override fun valueToString(): String = "[ ${value.joinToString(", ")} ]"

}