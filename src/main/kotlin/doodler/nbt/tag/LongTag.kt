package doodler.nbt.tag

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import doodler.nbt.AnyTag
import doodler.nbt.Tag
import doodler.nbt.TagType.TAG_LONG
import java.nio.ByteBuffer

@Stable
class LongTag private constructor(name: String? = null, parent: AnyTag?): Tag<Long>(TAG_LONG, name, parent) {

    override val sizeInBytes get() = Long.SIZE_BYTES

    constructor(value: Long, name: String? = null, parent: AnyTag?): this(name, parent) {
        valueState = mutableStateOf(value)
    }

    constructor(buffer: ByteBuffer, name: String? = null, parent: AnyTag?): this(name, parent) {
        read(buffer)
    }

    override fun read(buffer: ByteBuffer) {
        valueState = mutableStateOf(buffer.long)
    }

    override fun write(buffer: ByteBuffer) {
        buffer.putLong(value)
    }

    override fun clone(name: String?) = LongTag(value, name, parent)

}