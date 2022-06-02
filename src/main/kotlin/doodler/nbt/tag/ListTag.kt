package doodler.nbt.tag

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import doodler.extensions.contentEquals
import doodler.nbt.AnyTag
import doodler.nbt.Tag
import doodler.nbt.TagType
import doodler.nbt.TagType.*
import doodler.nbt.extensions.indent
import java.nio.ByteBuffer

@Stable
class ListTag(
    name: String? = null,
    parent: AnyTag?,
    elementsType: TagType,
    value: SnapshotStateList<AnyTag>? = null,
    buffer: ByteBuffer? = null
): Tag<SnapshotStateList<AnyTag>>(
    TAG_LIST, name, parent, value, buffer, elementsType
) {

    override val sizeInBytes: Int
        get() = Byte.SIZE_BYTES + Int.SIZE_BYTES + value.sumOf { it.sizeInBytes }

    var elementsType by mutableStateOf(elementsType)

    operator fun get(index: Int) = value[index]

    private fun typeCheck(elementsType: TagType, list: List<AnyTag>) = list.all { it.type == elementsType }

    override fun read(buffer: ByteBuffer, vararg extras: Any?): SnapshotStateList<AnyTag> =
        MutableList(buffer.int) { read(extras[0] as TagType, buffer, null, this) }.toMutableStateList()

    override fun write(buffer: ByteBuffer) {
        buffer.put(elementsType.id)
        buffer.putInt(value.size)
        value.forEach { it.write(buffer) }
    }

    override fun clone(name: String?) =
        ListTag(name, parent, elementsType, value = value.map { it.clone(null) }.toMutableStateList())
            .also { require(it.typeCheck(it.elementsType, it.value)) { "ListTag's elements must be of a single type" } }

    override fun valueToString(): String = if (value.isEmpty()) "[]" else "[\n${value.joinToString(",\n") { it.toString() }.indent()}\n]"

    override fun valueEquals(other: AnyTag): Boolean {
        if (javaClass != other.javaClass) return false

        other as ListTag

        return value.contentEquals(other.value)
    }

    override fun valueHashcode(): Int = value.hashCode()

}