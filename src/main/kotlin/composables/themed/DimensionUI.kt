package composables.themed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DimensionCategory(
    name: String,
    dir: String? = null
) {
    Row(modifier = Modifier.padding(start = 25.dp, end = 25.dp, top = 30.dp, bottom = 10.dp)) {
        Text(name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        if (dir != null) {
            Text(
                " $dir", fontSize = 16.sp, color = Color(255, 255, 255, 125),
                modifier = Modifier.align(Alignment.Bottom).padding(start = 7.dp)
            )
        }
    }
}

@Composable
fun DimensionItem(
    name: String,
    path: String? = null
) {
    TextButton(
        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
        onClick = { }
    ) {
        Row(horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth().padding(start = 35.dp)) {
            Text(name, fontSize = 24.sp, color = Color(255, 255, 255, 200))
            if (path != null) {
                Text(
                    " $path", fontSize = 16.sp, color = Color(255, 255, 255, 100),
                    modifier = Modifier.align(Alignment.Bottom).padding(start = 7.dp)
                )
            }
        }
    }
}