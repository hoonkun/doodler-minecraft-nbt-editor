package composables.stateless.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            onAddWorld("", "")
        }
        LinkText(
            "...or single NBT file",
            color = ThemedColor.from(ThemedColor.Bright, alpha = 195),
            fontSize = 22.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            onAddSingle("", "")
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
