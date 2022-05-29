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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import composables.global.JetBrainsMono
import composables.global.ThemedColor
import java.io.File


@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun BoxScope.Selector(onSelect: (File) -> Unit = { }) {

    val basePath by remember { mutableStateOf(System.getProperty("user.home")) }
    var text by remember { mutableStateOf(TextFieldValue("/", selection = TextRange(1))) }

    var ctrl by remember { mutableStateOf(false) }

    var candidateParentFile by remember { mutableStateOf(File("$basePath${text.text}")) }
    var candidateFiles by remember {
        mutableStateOf(
            if (candidateParentFile.isDirectory)
                candidateParentFile.listFiles().toList().sortedBy { file -> file.name }
                    .sortedBy { file -> if (file.isDirectory && !file.isFile) -1 else if (file.isFile) 1 else 2 }
            else listOf()
        )
    }

    var completeTargetFile by remember { mutableStateOf<File?>(null) }
    var completingText by remember { mutableStateOf<String?>(null) }

    val adjustedColumns = listOf(0, 0, 1, 0)

    var selected by remember { mutableStateOf<File?>(candidateParentFile) }

    val requester by remember { mutableStateOf(FocusRequester()) }

    val updateCompletingText = {
        val enteringFileName = text.text.substring(text.text.lastIndexOf('/') + 1, text.text.length)
        val lastFileName = completeTargetFile!!.name
        completingText = lastFileName.substring(enteringFileName.length until lastFileName.length)
    }

    val onTextValueUpdated: (TextFieldValue) -> Unit = {
        val path = "$basePath${it.text}"
        val newFile = File(path)
        selected = newFile.let { file -> if (file.exists()) file else null }
        if (!path.endsWith(".")) {
            val newAutoCompleteFile = File(path.let { str -> str.substring(0, str.lastIndexOf('/')) })
            if (newAutoCompleteFile.exists()) candidateParentFile = newAutoCompleteFile
            candidateFiles =
                if (candidateParentFile.isDirectory) candidateParentFile.listFiles().toList()
                    .filter { file -> file.absolutePath.contains("$basePath${text.text}") }
                    .sortedBy { file -> file.name }
                    .sortedBy { file -> if (file.isDirectory && !file.isFile) -1 else if (file.isFile) 1 else 2 }
                else listOf()
        }
        completeTargetFile =
            if (candidateFiles.size == 1) candidateFiles[0]
            else null
        if (completeTargetFile == null) completingText = null
        if (candidateFiles.size == 1) {
            updateCompletingText()
        }
    }

    val autoComplete = autoComplete@ {
        if (completeTargetFile == null) return@autoComplete false
        val newValue = "${text.text}$completingText${if (completeTargetFile!!.isDirectory) "/" else ""}"
        text = TextFieldValue(
            newValue,
            selection = TextRange(newValue.length)
        )
        onTextValueUpdated(text)
        completeTargetFile = null
        completingText = null
        true
    }

    val onKeyEvent: (KeyEvent) -> Boolean = onKeyEvent@ {
        if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
            if (completeTargetFile != null && candidateFiles.size == 1) {
                autoComplete()
            } else {
                requester.requestFocus()
                if (candidateFiles.isEmpty()) return@onKeyEvent true
                val noneSelected = completeTargetFile == null
                val lastFile =
                    completeTargetFile?.absolutePath == candidateFiles.last().absolutePath
                completeTargetFile =
                    if (noneSelected || lastFile) candidateFiles[0]
                    else candidateFiles[candidateFiles.indexOf(completeTargetFile) + 1]

                val enteringFileName = text.text.substring(text.text.lastIndexOf('/') + 1, text.text.length)
                val lastFileName = completeTargetFile!!.name
                completingText = lastFileName.substring(enteringFileName.length until lastFileName.length)
            }
        } else if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
            if (!autoComplete() && selected != null && ctrl) onSelect(selected!!)
        } else if (it.key == Key.CtrlLeft || it.key == Key.CtrlRight) {
            ctrl = it.type == KeyEventType.KeyDown
        }
        true
    }

    SideEffect {
        requester.requestFocus()
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = 31.dp).requiredWidthIn(min = 790.dp)) {
        Column (modifier = Modifier.padding(start = 15.dp, end = 15.dp)) {
            Text("/** you can use '..' to go parent directory */", color = Color(0xFF629755), fontFamily = JetBrainsMono, fontSize = 18.sp)
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
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    "${text.text}${completingText ?: ""}".let { TextFieldValue(it, selection = TextRange(it.length)) },
                    onValueChange = { },
                    readOnly = true,
                    textStyle = TextStyle(
                        color = ThemedColor.Editor.Tag.General,
                        fontSize = 23.sp,
                        fontFamily = JetBrainsMono
                    ),
                    cursorBrush = SolidColor(Color.Transparent),
                    modifier = Modifier.fillMaxWidth().alpha(0.4f),
                    singleLine = true
                )
                BasicTextField(
                    text,
                    onValueChange = {
                        text = it
                        onTextValueUpdated(it)
                    },
                    textStyle = TextStyle(
                        color = ThemedColor.Editor.Tag.General,
                        fontSize = 23.sp,
                        fontFamily = JetBrainsMono
                    ),
                    cursorBrush = SolidColor(ThemedColor.Editor.Tag.General),
                    modifier = Modifier.fillMaxWidth()
                        .onKeyEvent(onKeyEvent)
                        .focusRequester(requester),
                    singleLine = true
                )
            }
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

            if (candidateParentFile.isDirectory) {
                val columns = 4
                val childDirectories = candidateFiles
                    .filter { it.isDirectory && !it.isFile }
                    .chunked(columns)
                val childFiles = candidateFiles
                    .filter { it.isFile || (!it.isDirectory && !it.isFile) }
                    .chunked(columns)

                val chunkedDirectoryIndex = childDirectories.indexOf(
                    childDirectories.find { chunked -> chunked.find { it == completeTargetFile } != null }
                ).minus(1).coerceIn(0, (childDirectories.size - 3).coerceAtLeast(0))
                val chunkedFileIndex = childFiles.indexOf(
                    childFiles.find { chunked -> chunked.find { it == completeTargetFile } != null }
                ).minus(1).coerceIn(0, (childFiles.size - 3).coerceAtLeast(0))

                val displayingDirectories = childDirectories.slice(chunkedDirectoryIndex until (chunkedDirectoryIndex + 3).coerceAtMost(childDirectories.size))
                val displayingFiles = childFiles.slice(chunkedFileIndex until (chunkedFileIndex + 3).coerceAtMost(childFiles.size))

                Spacer(modifier = Modifier.height(15.dp))

                if (displayingDirectories.isNotEmpty()) {
                    for (dirsChunked in displayingDirectories) {
                        Row {
                            for (dir in dirsChunked) {
                                CandidateText(dir.name, color = Color(0xFFFFC66D), dir == completeTargetFile)
                                Spacer(modifier = Modifier.width(15.dp))
                            }
                            for (dummy in 0 until adjustedColumns[dirsChunked.size - 1]) {
                                Spacer(modifier = Modifier.weight(1f))
                                Spacer(modifier = Modifier.width(15.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                    }
                    RemainingItems(childDirectories, chunkedDirectoryIndex, Color(0x90FFC66D))

                    Spacer(modifier = Modifier.height(25.dp))
                }

                if (displayingFiles.isNotEmpty()) {
                    for (filesChunked in displayingFiles) {
                        Row {
                            for (fileEach in filesChunked) {
                                CandidateText(
                                    fileEach.name,
                                    color = ThemedColor.Editor.Tag.General,
                                    fileEach == completeTargetFile
                                )
                                Spacer(modifier = Modifier.width(15.dp))
                            }
                            for (dummy in 0 until adjustedColumns[filesChunked.size - 1]) {
                                Spacer(modifier = Modifier.weight(1f))
                                Spacer(modifier = Modifier.width(15.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                    }
                    RemainingItems(childFiles, chunkedFileIndex, ThemedColor.from(ThemedColor.Editor.Tag.General, alpha = 144))
                }
            }
        }
    }
}

@Composable
fun RowScope.CandidateText(text: String, color: Color, focused: Boolean) {
    Text(
        text = text,
        color = color,
        fontFamily = JetBrainsMono,
        fontSize = 18.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f).let { if (focused) it.background(Color(0x20FFFFFF)) else it }
    )
}

@Composable
fun RemainingItems(list: List<List<File>>, startIndex: Int, color: Color) {
    val lastIndex = (startIndex + 3).coerceAtMost(list.size)
    if (list.size > lastIndex) {
        val remaining = list.slice(lastIndex until list.size).sumOf { it.size }
        Text(
            "...$remaining items more",
            color = color,
            fontFamily = JetBrainsMono,
            fontSize = 18.sp,
            maxLines = 1
        )
    }
}
