package doodler.nbt.tag

import androidx.compose.runtime.Stable
import doodler.nbt.Tag
import doodler.nbt.TagType.*
import java.nio.ByteBuffer

@Stable
class EndTag: Tag<Nothing>(TAG_END, null, null) {

    override val sizeInBytes = 0

    override fun read(buffer: ByteBuffer) { }

    override fun write(buffer: ByteBuffer) { }

    override fun clone(name: String?) = EndTag()

}