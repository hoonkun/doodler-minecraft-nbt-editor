package activator.doodler.nbt.tag

import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import activator.doodler.extensions.contentEquals
import activator.doodler.nbt.AnyTag
import activator.doodler.nbt.Tag
import activator.doodler.nbt.TagType
import activator.doodler.nbt.TagType.*
import activator.doodler.nbt.extensions.byte
import activator.doodler.nbt.extensions.putString
import activator.doodler.nbt.extensions.string
import java.nio.ByteBuffer

typealias Compound = SnapshotStateList<AnyTag>

@Stable
class CompoundTag(
    name: String? = null,
    parent: AnyTag?,
    value: Compound? = null,
    buffer: ByteBuffer? = null
): Tag<Compound>(TAG_COMPOUND, name, parent, value, buffer) {

    override val sizeInBytes: Int
        get() = value.sumOf { tag ->
            Byte.SIZE_BYTES + Short.SIZE_BYTES + (tag.name ?: "").toByteArray().size + tag.sizeInBytes
        } + Byte.SIZE_BYTES

    operator fun get(key: Int) = value[key]
    operator fun get(key: String) = value.find { it.name == key }

    operator fun set(key: Int, nv: AnyTag) {
        value[key] = nv
    }

    fun insert(index: Int, nv: AnyTag) {
        value.add(index, nv)
    }

    fun add(nv: AnyTag) {
        value.add(nv)
    }

    fun remove(name: String?) {
        if (name == null) return

        value.removeIf { it.name == name }
    }

    override fun read(buffer: ByteBuffer, vararg extras: Any?): Compound {
        val new = mutableListOf<AnyTag>()

        var nextId: Byte
        do {
            nextId = buffer.byte

            if (nextId == TAG_END.id) break

            val nextName = buffer.string
            val nextTag = read(TagType[nextId], buffer, nextName, this)

            new.add(nextTag)
        } while (true)

        return new.toMutableStateList()
    }

    override fun write(buffer: ByteBuffer) {
        value.forEach { tag ->
            buffer.put(tag.type.id)
            buffer.putString(tag.name ?: "")

            tag.write(buffer)
        }

        buffer.put(TAG_END.id)
    }

    fun writeAsRoot(buffer: ByteBuffer) {
        buffer.put(TAG_COMPOUND.id)
        buffer.putString(name ?: "")
        write(buffer)
    }

    override fun clone(name: String?) = CompoundTag(name, parent, value = value.map { tag -> tag.clone(tag.name) }.toMutableStateList())

    override fun valueToString(): String =
        "{\n${value.sortedBy { it.name ?: "" }.joinToString(",\n") { "${it.value}" }}\n}"

    override fun valueEquals(other: AnyTag): Boolean {
        if (javaClass != other.javaClass) return false

        other as CompoundTag

        return value.contentEquals(other.value)
    }

    override fun valueHashcode(): Int = value.hashCode()

}