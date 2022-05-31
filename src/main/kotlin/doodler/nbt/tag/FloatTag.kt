package doodler.nbt.tag

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import doodler.nbt.AnyTag
import doodler.nbt.Tag
import doodler.nbt.TagType.TAG_FLOAT
import java.nio.ByteBuffer

@Stable
class FloatTag private constructor(name: String? = null, parent: AnyTag?): Tag<Float>(TAG_FLOAT, name, parent) {

    override val sizeInBytes get() = Float.SIZE_BYTES

    constructor(value: Float, name: String? = null, parent: AnyTag?): this(name, parent) {
        valueState = mutableStateOf(value)
    }

    constructor(buffer: ByteBuffer, name: String? = null, parent: AnyTag?): this(name, parent) {
        read(buffer)
    }

    override fun read(buffer: ByteBuffer) {
        valueState = mutableStateOf(buffer.float)
    }

    override fun write(buffer: ByteBuffer) {
        buffer.putFloat(value)
    }

    override fun clone(name: String?) = FloatTag(value, name, parent)

}