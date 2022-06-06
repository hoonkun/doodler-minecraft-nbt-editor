package doodler.minecraft

import doodler.nbt.Tag
import doodler.nbt.TagType
import doodler.nbt.extensions.byte
import doodler.nbt.tag.CompoundTag
import java.io.File
import java.nio.ByteBuffer

class DatWorker {

    companion object {

        fun read(bytes: ByteArray): CompoundTag {
            val uncompressed = CompressUtils.GZip.decompress(bytes)
            return Tag.read(TagType.TAG_COMPOUND, ByteBuffer.wrap(uncompressed).apply { byte; short; }, null, null).getAs()
        }

        fun write(root: CompoundTag, file: File) {
            val buffer = ByteBuffer.allocate(root.sizeInBytes)
            root.writeAsRoot(buffer)
            file.writeBytes(CompressUtils.GZip.compress(buffer.array()))
        }

    }

}