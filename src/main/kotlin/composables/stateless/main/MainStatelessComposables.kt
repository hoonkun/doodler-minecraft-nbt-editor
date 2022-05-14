package composables.stateless.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import composables.themed.LinkText
import composables.themed.ThemedColor

@Composable
fun ColumnScope.NoWorldsSelected(
    onAddWorld: (String, String) -> Unit,
    onAddSingle: (String, String) -> Unit
) {
    Column (
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(43, 43, 43))
            .weight(1.0f)
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            "Doodler: Minecraft NBT Editor",
            color = Color(255, 255, 255, 125),
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
            onAddWorld("", "")
        }
        LinkText(
            "...or single NBT file",
            color = Color(174, 213, 129, 195),
            fontSize = 22.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            onAddSingle("", "")
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
            color = ThemedColor.Link,
            fontSize = 22.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {

        }
        Spacer(modifier = Modifier.weight(1f))
    }
}