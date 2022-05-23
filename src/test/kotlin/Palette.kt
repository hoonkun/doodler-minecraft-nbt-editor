import androidx.compose.ui.graphics.Color
import java.io.File
import javax.imageio.ImageIO

class Palette {

    fun start(file: File): Triple<Int, Int, Int>? {
        val stream = ImageIO.createImageInputStream(file)
        val iterator = ImageIO.getImageReaders(stream)

        if (!iterator.hasNext()) {
            return null
        }

        val reader = iterator.next()
        reader.input = stream

        val image = reader.read(0)

        val width = image.width
        val height = image.height

        var r = 0f
        var g = 0f
        var b = 0f
        var invalids = 0
        for (x in 0 until width) {
            for (y in 0 until height) {
                val color = Color(image.getRGB(x, y))
                if (color.alpha != 1f) {
                    invalids++
                    continue
                }
                r += color.red * 255
                g += color.green * 255
                b += color.blue * 255
            }
        }

        val pixels = width * height - invalids
        return Triple((r / pixels).toInt(), (g / pixels).toInt(), (b / pixels).toInt())
    }

}