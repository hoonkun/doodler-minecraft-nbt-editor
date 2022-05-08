import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowState
import composables.themed.LinkText

@Composable
@Preview
fun App() {
    var worlds by remember { mutableStateOf(mutableListOf<String>()) }
    var world by remember { mutableStateOf(-1) }

    MaterialTheme {
        Column (modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                elevation = 10.dp,
                backgroundColor = Color(104, 159, 56),
                contentPadding = PaddingValues(start = 25.dp, top = 15.dp, bottom = 10.dp)
            ) {
                Text (
                    "Doodler: Minecraft NBT Editor",
                    color = Color.White,
                    fontSize = 32.sp
                )
            }
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(43, 43, 43))
                    .weight(1.0f)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "No World Selected.",
                    color = Color(255, 255, 255, 125),
                    fontSize = 30.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(20.dp))
                LinkText(
                    "Select World",
                    color = Color(174, 213, 129),
                    fontSize = 40.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {

                }
                LinkText(
                    "...or single NBT file",
                    color = Color(174, 213, 129, 195),
                    fontSize = 22.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {

                }
                Spacer(modifier = Modifier.height(80.dp))
                Text(
                    "Getting started?",
                    color = Color(255, 255, 255, 85),
                    fontSize = 26.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(10.dp))
                LinkText(
                    "Documentation",
                    color = Color(100, 181, 246, 200),
                    fontSize = 22.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {

                }
                Spacer(modifier = Modifier.weight(1f))
            }
            BottomAppBar(
                elevation = 10.dp,
                backgroundColor = Color(60, 63, 65),
                contentPadding = PaddingValues(top = 0.dp, bottom = 0.dp, start = 25.dp, end = 25.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "by kiwicraft",
                    color = Color(255, 255, 255, 180),
                    fontSize = 14.sp
                )
            }
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        state = WindowState(
            size = DpSize(1400.dp, 1100.dp)
        )
    ) {
        App()
    }
}
