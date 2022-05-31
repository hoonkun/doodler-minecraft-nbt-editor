package doodler.nbt.tag

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import doodler.nbt.AnyTag
import doodler.nbt.Tag
import doodler.nbt.TagType.TAG_STRING
import doodler.nbt.extensions.putString
import doodler.nbt.extensions.string
import java.nio.ByteBuffer

@Stable
class StringTag private constructor(name: String? = null, parent: AnyTag?): Tag<String>(TAG_STRING, name, parent) {

    override val sizeInBytes get() = Short.SIZE_BYTES * value.toByteArray().size

    constructor(value: String, name: String? = null, parent: AnyTag?): this(name, parent) {
        valueState = mutableStateOf(value)
    }

    constructor(buffer: ByteBuffer, name: String? = null, parent: AnyTag?): this(name, parent) {
        read(buffer)
    }

    override fun read(buffer: ByteBuffer) {
        valueState = mutableStateOf(buffer.string)
    }

    override fun write(buffer: ByteBuffer) {
        buffer.putString(value)
    }

    override fun clone(name: String?): Tag<String> = StringTag(value, name, parent)

    override fun valueToString(): String = "\"$value\""

}