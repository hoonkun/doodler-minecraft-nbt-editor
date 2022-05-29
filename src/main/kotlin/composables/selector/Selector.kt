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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
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
    var value by remember { mutableStateOf(TextFieldValue("/", selection = TextRange(1))) }

    var ctrl by remember { mutableStateOf(false) }
    var shift by remember { mutableStateOf(false) }

    var candidateParentFile by remember { mutableStateOf(File("$basePath${value.text}")) }
    val candidateFiles by remember(candidateParentFile, value, basePath) {
        mutableStateOf(
            if (candidateParentFile.isDirectory)
                candidateParentFile.listFiles().toList()
                    .filter { file -> file.absolutePath.contains("$basePath${value.text}") }
                    .sortedBy { file -> file.name }
                    .sortedBy { file -> if (file.isDirectory && !file.isFile) -1 else if (file.isFile) 1 else 2 }
            else listOf()
        )
    }

    var completeTargetFile by remember(candidateFiles) {
        mutableStateOf(if (candidateFiles.size == 1) candidateFiles[0] else null)
    }
    val completingText by remember(completeTargetFile, value.text) {
        if (completeTargetFile == null) return@remember mutableStateOf<String?>(null)

        val enteringFileName = value.text.substring(value.text.lastIndexOf('/') + 1, value.text.length)
        val lastFileName = completeTargetFile!!.name

        mutableStateOf<String?>(lastFileName.substring(enteringFileName.length until lastFileName.length))
    }

    val adjustedColumns = listOf(0, 0, 1, 0)

    val selected by remember(basePath, value.text) {
        mutableStateOf(File("$basePath${value.text}").let { file -> if (file.exists()) file else null })
    }

    val displayValue by remember(value, completingText) {
        val newString = "${value.text}${completingText ?: ""}"
        mutableStateOf(TextFieldValue(
            AnnotatedString(
                newString,
                listOf(AnnotatedString.Range(
                    SpanStyle(color = ThemedColor.from(ThemedColor.Editor.Tag.General, alpha = 80)),
                    value.text.length,
                    newString.length
                ))
            ),
            selection = TextRange(value.selection.start)
        ))
    }

    val requester by remember { mutableStateOf(FocusRequester()) }

    val remap: (TextFieldValue) -> TextFieldValue = remap@ {
        val newString = it.text.removeSuffix(completingText ?: "")
        TextFieldValue(newString, selection = TextRange(it.selection.start.coerceAtMost(newString.length)))
    }

    val updateCandidateParent = updateCandidateParent@ {
        val path = "$basePath${value.text}"
        if (path.endsWith(".")) return@updateCandidateParent

        val newAutoCompleteFile = File(path.let { str -> str.substring(0, str.lastIndexOf('/')) })
        if (newAutoCompleteFile.exists()) candidateParentFile = newAutoCompleteFile
    }

    val onTextValueUpdated: (TextFieldValue) -> Unit = {
        value = remap(it)
        updateCandidateParent()
    }

    val autoComplete = autoComplete@ {
        if (completeTargetFile == null) return@autoComplete false
        val newValue = "${value.text}$completingText${if (completeTargetFile!!.isDirectory) "/" else ""}"
        value = TextFieldValue(
            newValue,
            selection = TextRange(newValue.length)
        )
        updateCandidateParent()
        completeTargetFile = null
        true
    }

    val onKeyEvent: (KeyEvent) -> Boolean = onKeyEvent@ {
        if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
            if (completeTargetFile != null && candidateFiles.size == 1) {
                if (shift) return@onKeyEvent true
                autoComplete()
            } else {
                requester.requestFocus()
                if (candidateFiles.isEmpty()) return@onKeyEvent true
                val noneSelected = completeTargetFile == null
                val lastFile = completeTargetFile?.absolutePath == candidateFiles.last().absolutePath
                val firstFile = completeTargetFile?.absolutePath == candidateFiles.first().absolutePath
                completeTargetFile =
                    if (!shift) {
                        if (noneSelected || lastFile) candidateFiles.first()
                        else candidateFiles[candidateFiles.indexOf(completeTargetFile) + 1]
                    } else {
                        if (noneSelected || firstFile) candidateFiles.last()
                        else candidateFiles[candidateFiles.indexOf(completeTargetFile) - 1]
                    }
            }
        } else if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
            if (!autoComplete() && selected != null && ctrl) onSelect(selected!!)
        } else if (it.key == Key.CtrlLeft || it.key == Key.CtrlRight) {
            ctrl = it.type == KeyEventType.KeyDown
        } else if (it.key == Key.ShiftLeft || it.key == Key.ShiftRight) {
            shift = it.type == KeyEventType.KeyDown
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
            BasicTextField(
                displayValue,
                onValueChange = onTextValueUpdated,
                textStyle = TextStyle(
                    color = ThemedColor.Editor.Tag.General,
                    fontSize = 23.sp,
                    fontFamily = JetBrainsMono
                ),
                cursorBrush = SolidColor(ThemedColor.Editor.Tag.General),
                modifier = Modifier.weight(1f)
                    .onKeyEvent(onKeyEvent)
                    .focusRequester(requester),
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
                                CandidateText(
                                    dir.name,
                                    color = Color(0xFFFFC66D),
                                    dir == completeTargetFile && displayingDirectories.size != 1
                                )
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
                                    fileEach == completeTargetFile && displayingFiles.size != 1
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
    Box(modifier = Modifier.weight(1f)) {
        Text(
            text = text,
            color = color,
            fontFamily = JetBrainsMono,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.let { if (focused) it.background(Color(0x30FFFFFF)) else it }
        )
    }
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
