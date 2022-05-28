package composables.selector

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.mouseClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import composables.global.JetBrainsMono
import composables.global.ThemedColor
import java.io.File


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BoxScope.Selector(onSelect: (File) -> Unit = { }) {

    val basePath by remember { mutableStateOf(System.getProperty("user.home")) }
    var text by remember { mutableStateOf(TextFieldValue("/")) }

    var autoCompleteFile by remember { mutableStateOf(File("$basePath${text.text}")) }

    val adjustedColumns = listOf(0, 0, 1, 0)

    var selected by remember { mutableStateOf<File?>(autoCompleteFile) }

    Column(modifier = Modifier.fillMaxSize().padding(top = 31.dp).requiredWidthIn(min = 790.dp)) {
        Column (modifier = Modifier.padding(start = 15.dp, end = 15.dp)) {
            Text("/** you can use '..' to go parent dir */", color = Color(0xFF629755), fontFamily = JetBrainsMono, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "basePath = ",
                    color = ThemedColor.ChunkSelectorPropertyKey,
                    fontFamily = JetBrainsMono,
                    fontSize = 22.sp
                )
                Text(basePath, color = ThemedColor.Editor.Tag.General, fontFamily = JetBrainsMono, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(32, 32, 32), RoundedCornerShape(5.dp)).height(60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(15.dp))
            BasicTextField(
                text,
                onValueChange = {
                    text = it
                    val path = "$basePath${it.text}"
                    val newFile = File(path)
                    selected = newFile.let { file -> if (file.exists()) file else null }
                    if (!path.endsWith(".")) {
                        val newAutoCompleteFile = File(path.let { str -> str.substring(0, str.lastIndexOf('/')) })
                        if (newAutoCompleteFile.exists()) autoCompleteFile = newAutoCompleteFile
                    }
                },
                textStyle = TextStyle(
                    color = ThemedColor.Editor.Tag.General,
                    fontSize = 23.sp,
                    fontFamily = JetBrainsMono
                ),
                cursorBrush = SolidColor(ThemedColor.Editor.Tag.General),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(15.dp))
            Box (
                modifier = Modifier.width(80.dp).fillMaxHeight().padding(5.dp)
                    .mouseClickable {
                        if (buttons.isPrimaryPressed && selected != null) {
                            onSelect(selected!!)
                        }
                    }
            ) {
                Box (modifier = Modifier.background(Color(0xff55783d), RoundedCornerShape(5.dp)).fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Go!", fontFamily = JetBrainsMono, color = Color(0xFFCCCCCC), fontSize = 22.sp)
                }
            }
        }
        Column (modifier = Modifier.padding(start = 15.dp, end = 15.dp)) {
            val breakIfOverflow: @Composable (List<List<File>>, List<File>, Color) -> Boolean =
                { entire, chunked, color ->
                    if (entire.indexOf(chunked) == 3) {
                        val remaining = entire.slice(3 until entire.size).sumOf { it.size }
                        Text(
                            "...$remaining items more",
                            color = color,
                            fontFamily = JetBrainsMono,
                            fontSize = 18.sp,
                            maxLines = 1
                        )
                    }
                    entire.indexOf(chunked) == 3
                }

            if (autoCompleteFile.isDirectory) {
                val children = autoCompleteFile.listFiles()
                val columns = 4
                val childDirectories = children
                    .filter { it.isDirectory && it.absolutePath.contains("$basePath${text.text}") }
                    .sortedBy { it.name }
                    .chunked(columns)
                val childFiles = children
                    .filter { it.isFile && it.absolutePath.contains("$basePath${text.text}") }
                    .sortedBy { it.name }
                    .chunked(columns)

                Spacer(modifier = Modifier.height(15.dp))

                for (dirsChunked in childDirectories) {
                    if (breakIfOverflow(childDirectories, dirsChunked, Color(0x90FFC66D))) break
                    Row {
                        for (dir in dirsChunked) {
                            AutoCompleteText(dir.name, color = Color(0xFFFFC66D))
                            Spacer(modifier = Modifier.width(15.dp))
                        }
                        for (dummy in 0 until adjustedColumns[dirsChunked.size - 1]) {
                            Spacer(modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(15.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                }

                Spacer(modifier = Modifier.height(25.dp))

                for (filesChunked in childFiles) {
                    if (breakIfOverflow(
                            childFiles,
                            filesChunked,
                            ThemedColor.from(ThemedColor.Editor.Tag.General, alpha = 144)
                        )
                    ) break
                    Row {
                        for (fileEach in filesChunked) {
                            AutoCompleteText(fileEach.name, color = ThemedColor.Editor.Tag.General)
                            Spacer(modifier = Modifier.width(15.dp))
                        }
                        for (dummy in 0 until adjustedColumns[filesChunked.size - 1]) {
                            Spacer(modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(15.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                }
            }
        }
    }
}

@Composable
fun RowScope.AutoCompleteText(text: String, color: Color) {
    Text(text = text, color = color, fontFamily = JetBrainsMono, fontSize = 18.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
}
