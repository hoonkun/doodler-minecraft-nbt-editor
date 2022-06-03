import androidx.compose.foundation.layout.Box
import androidx.compose.material.Text
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {

    Window(onCloseRequest = ::exitApplication) {
        Box {
            Text("Hello, Doodler!")
        }
    }

}
