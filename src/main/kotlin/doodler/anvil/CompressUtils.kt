package doodler.anvil

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

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