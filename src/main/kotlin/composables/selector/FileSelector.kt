package composables.selector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import composables.global.ThemedColor
import java.io.File

@Composable
fun FileSelector(onSelect: (String, String) -> Unit) {

    val selected = mutableStateOf<File?>(null)

    val onFileSelect = open@ {
        val file = selected.value ?: return@open

        onSelect(file.name, file.absolutePath)
    }

    Box(modifier = Modifier.background(ThemedColor.EditorArea).padding(start = 30.dp, end = 30.dp)) {
        Selector(selected)
    }
}