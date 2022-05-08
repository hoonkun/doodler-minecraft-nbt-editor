package composables.themed

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DimensionCategory(
    name: String,
    dir: String? = null,
    initialFolded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = { }
) {
    var folded by remember { mutableStateOf(initialFolded) }

    Column {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(start = 25.dp, end = 25.dp, top = 30.dp, bottom = 10.dp)
        ) {
            Text(name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            if (dir != null) {
                Text(
                    " $dir", fontSize = 16.sp, color = Color(255, 255, 255, 125),
                    modifier = Modifier.align(Alignment.Bottom).padding(start = 7.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            LinkText(
                if (folded) "unfold" else "fold",
                ThemedColor.Bright,
                19.sp
            ) { folded = !folded }
        }
        if (!folded) content()
    }
}


@Composable
fun DimensionItem(
    name: String,
    path: String? = null,
    onClick: (String) -> Unit = { }
) {
    ListItem (onClick = { onClick(name) }) {
        Row(horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth().padding(start = 35.dp, end = 18.dp)) {
            Text(name, fontSize = 24.sp, color = Color(255, 255, 255, 200))
            if (path != null) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    " $path", fontSize = 16.sp, color = Color(255, 255, 255, 100),
                    modifier = Modifier.align(Alignment.Bottom).padding(start = 7.dp)
                )
            }
        }
    }
}