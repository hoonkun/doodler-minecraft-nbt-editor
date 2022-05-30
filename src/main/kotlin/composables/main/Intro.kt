package composables.main

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.BottomAppBar
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import composables.global.LinkText
import composables.global.ThemedColor
import doodler.application.structures.DoodlerWindow
import doodler.logger.DoodlerLogger

@Composable
@Preview
fun Intro(
    onOpen: (DoodlerWindow.Type) -> Unit
) {

    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            NoWorldsSelected(onOpen)
            BottomAppBar(
                elevation = 15.dp,
                backgroundColor = ThemedColor.TaskArea,
                contentPadding = PaddingValues(top = 0.dp, bottom = 0.dp, start = 25.dp, end = 25.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "by kiwicraft",
                    color = ThemedColor.Copyright,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun ColumnScope.NoWorldsSelected(
    onOpen: (DoodlerWindow.Type) -> Unit
) {
    DoodlerLogger.recomposition("NoWorldsSelected")

    Column (
        modifier = Modifier
            .fillMaxWidth()
            .background(ThemedColor.EditorArea)
            .weight(1.0f)
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            "Doodler: Minecraft NBT Editor",
            color = ThemedColor.WhiteSecondary,
            fontSize = 30.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(20.dp))
        LinkText(
            "Select World",
            color = ThemedColor.Bright,
            fontSize = 40.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            onOpen(DoodlerWindow.Type.WORLD_SELECTOR)
        }
        LinkText(
            "...or single NBT file",
            color = ThemedColor.from(ThemedColor.Bright, alpha = 195),
            fontSize = 22.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            onOpen(DoodlerWindow.Type.FILE_SELECTOR)
        }
        Spacer(modifier = Modifier.height(80.dp))
        Text(
            "Getting started?",
            color = ThemedColor.DocumentationDescription,
            fontSize = 26.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(10.dp))
        LinkText(
            "Documentation",
            color = ThemedColor.Link,
            fontSize = 22.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {

        }
        Spacer(modifier = Modifier.weight(1f))
    }
}
