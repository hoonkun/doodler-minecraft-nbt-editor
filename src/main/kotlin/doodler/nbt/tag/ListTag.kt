package doodler.nbt.tag

import doodler.nbt.AnyTag
import doodler.nbt.Tag
import doodler.nbt.TagType
import doodler.nbt.TagType.*
import doodler.nbt.extensions.byte
import doodler.nbt.extensions.indent
import java.nio.ByteBuffer

class ListTag private constructor(name: String? = null, parent: AnyTag?): Tag<MutableList<AnyTag>>(TAG_LIST, name, parent) {

    override val sizeInBytes: Int
        get() = Byte.SIZE_BYTES + Int.SIZE_BYTES + value.sumOf { it.sizeInBytes }

    lateinit var elementsType: TagType

    operator fun get(index: Int) = value[index]

    constructor(elementsType: TagType, value: List<AnyTag>, typeCheck: Boolean = true, name: String? = null, parent: AnyTag?): this(name, parent) {
        require(!typeCheck || typeCheck(elementsType, value)) { "ListTag's elements must be of a single type" }

        this.elementsType = elementsType
        this.value = value.map { tag -> tag.ensureName(null) }.toMutableList()
    }

    constructor(buffer: ByteBuffer, name: String? = null, parent: AnyTag?): this(name, parent) {
        read(buffer)
    }

    private fun typeCheck(elementsType: TagType, list: List<AnyTag>) = list.all { it.type == elementsType }

    override fun read(buffer: ByteBuffer) {
        elementsType = TagType[buffer.byte]
        value = MutableList(buffer.int) { read(elementsType, buffer, null, this) }
    }

    override fun write(buffer: ByteBuffer) {
        buffer.put(elementsType.id)
        buffer.putInt(value.size)
        value.forEach { it.write(buffer) }
    }

    override fun clone(name: String?) = ListTag(elementsType, value.map { it.clone(null) }, false, name, parent)

    override fun valueToString(): String = if (value.isEmpty()) "[]" else "[\n${value.joinToString(",\n") { it.toString() }.indent()}\n]"

}