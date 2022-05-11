package doodler.anvil

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.Inflater

class Zlib {
    companion object {

        fun decompress(input: ByteArray): ByteArray {
            val inflater = Inflater()
            inflater.setInput(input)

            val outputArray = ByteArray(1024)
            val stream = ByteArrayOutputStream(input.size)
            while (!inflater.finished()) {
                val count = inflater.inflate(outputArray)
                stream.write(outputArray, 0, count)
            }

            val result = stream.toByteArray()
            stream.close()

            return result
        }

        fun compress(input: ByteArray): ByteArray {
            val deflater = Deflater()
            deflater.setInput(input)
            deflater.finish()

            val compressTemplate = ByteArray(1024)
            val compressStream = ByteArrayOutputStream()

            while (!deflater.finished()) {
                val count = deflater.deflate(compressTemplate)
                compressStream.write(compressTemplate, 0, count)
            }

            return compressStream.toByteArray()
        }

    }
}

class GZip {

    companion object {

        fun decompress(input: ByteArray): ByteArray {
            val inputStream = GZIPInputStream(BufferedInputStream(ByteArrayInputStream(input)))
            val outputStream = ByteArrayOutputStream()

            val buffer = ByteArray(1024)
            do {
                val size = inputStream.read(buffer)
                if (size > 0) outputStream.write(buffer, 0, size)
            } while (size > 0)

            outputStream.flush()
            outputStream.close()

            return outputStream.toByteArray()
        }

        fun compress(input: ByteArray): ByteArray {
            val outputStream = ByteArrayOutputStream()
            val gzip = GZIPOutputStream(BufferedOutputStream(outputStream))
            gzip.write(input)
            gzip.finish()
            gzip.close()

            return outputStream.toByteArray()
        }

    }

}