package doodler.nbt.tag

import androidx.compose.runtime.Stable
import doodler.doodle.DoodleException
import doodler.nbt.AnyTag
import doodler.nbt.Tag
import doodler.nbt.TagType.*
import java.nio.ByteBuffer

@Stable
class EndTag: Tag<Nothing>(TAG_END, null, null) {

    override val sizeInBytes = 0

    override fun read(buffer: ByteBuffer, vararg extras: Any?): Nothing =
        throw DoodleException("Internal Error", null, "cannot read TAG_END")

    override fun write(buffer: ByteBuffer) { }

    override fun clone(name: String?) = EndTag()

    override fun valueEquals(other: AnyTag): Boolean = javaClass == other.javaClass

    override fun valueHashcode(): Int = 0

}