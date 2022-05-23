package doodler.anvil

import kotlin.math.pow

typealias PackedBlocks = LongArray
typealias Blocks = List<Short>

class ArrayPacker {

    companion object {

        fun PackedBlocks.unpack(paletteSize: Int): Blocks {
            val bitsPerBlock = size(paletteSize)
            val bitMask = (2.0).pow(bitsPerBlock).toLong() - 1L

            val result = mutableListOf<Short>()

            forEach { long ->
                var remaining = long
                for (block in 0 until(Long.SIZE_BITS / bitsPerBlock)) {
                    result.add((remaining and bitMask).toShort())
                    remaining = remaining shr bitsPerBlock

                    if (result.size == 4096) return@forEach
                }
            }

            return result
        }

        private fun size(size: Int): Int {
            var result = 4
            var value = 2 * 2 * 2 * 2
            while (size > value) {
                value *= 2
                result++
            }
            return result
        }

    }

}