package nbt.tag

import nbt.AnyTag
import nbt.Tag
import nbt.TagType
import nbt.TagType.*
import nbt.extensions.byte
import nbt.extensions.putString
import nbt.extensions.string
import java.nio.ByteBuffer

typealias Compound = MutableMap<String, AnyTag>

class CompoundTag private constructor(name: String? = null, parent: AnyTag?): Tag<Compound>(TAG_COMPOUND, name, parent) {

    private var complicated = false

    override val sizeInBytes: Int
        get() = value.entries.sumOf { (name, tag) ->
            Byte.SIZE_BYTES + Short.SIZE_BYTES + name.toByteArray().size + tag.sizeInBytes
        } + Byte.SIZE_BYTES

    operator fun get(key: String) = value[key]
    operator fun set(key: String, nv: AnyTag) {
        value[key] = nv
    }

    constructor(value: Compound, name: String? = null, parent: AnyTag?): this(name, parent) {
        this.value = value.map { (name, tag) -> name to tag.ensureName(name) }.toMap().toMutableMap()
    }

    constructor(buffer: ByteBuffer, name: String? = null, parent: AnyTag?): this(name, parent) {
        read(buffer)
    }

    override fun read(buffer: ByteBuffer) {
        val new = mutableMapOf<String, AnyTag>()

        var nextId: Byte
        do {
            nextId = buffer.byte

            if (nextId == TAG_END.id) break

            val nextName = buffer.string
            val nextTag = read(TagType[nextId], buffer, nextName, this)

            new[nextName] = nextTag
        } while (true)

        value = new
    }

    override fun write(buffer: ByteBuffer) {
        value.entries.forEach { (name, tag) ->
            buffer.put(tag.type.id)
            buffer.putString(name)

            tag.write(buffer)
        }

        buffer.put(TAG_END.id)
    }

    fun writeAsRoot(buffer: ByteBuffer) {
        buffer.put(TAG_COMPOUND.id)
        buffer.putString(name ?: "")
        write(buffer)
    }

    override fun clone(name: String?) = CompoundTag(value.map { (name, tag) -> name to tag.clone(name) }.toMap().toMutableMap(), name, parent)

    override fun valueToString(): String {
        val result = "{\n${value.entries.sortedBy { it.key }.joinToString(",\n") { "${it.value}" }}\n}"
        return if (complicated) result else result.replace("\n", " ")
    }

    fun generateTypes(parentPath: String = ""): Map<String, Byte> {
        val result = mutableMapOf<String, Byte>()
        value.entries.map { (k, v) ->
            val path = if (parentPath == "") k else "$parentPath.$k"
            result[path] = v.type.id
            if (v.type == TAG_COMPOUND) {
                result.putAll(v.getAs<CompoundTag>().generateTypes(path))
            }
            if (v.type == TAG_LIST) {
                val listTag = v.getAs<ListTag>()
                result["$path.*"] = listTag.elementsType.id
                val listCompoundTypes = listTag.generateTypes(path)
                result.putAll(listCompoundTypes)
            }
        }
        return result
    }

}