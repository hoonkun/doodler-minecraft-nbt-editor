package composables.stateful.main

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.BottomAppBar
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import composables.stateless.main.NoWorldsSelected
import composables.themed.ThemedColor

@Composable
@Preview
fun Intro(
    addWorld: (String, String) -> Unit,
    addSingle: (String, String) -> Unit
) {

    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            NoWorldsSelected(addWorld, addSingle)
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