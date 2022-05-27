package doodler.minecraft

import kotlin.math.pow

class ArrayPacker {

    companion object {

        fun LongArray.unpack(paletteSize: Int): List<Short> {
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