package composables.selector

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.mouseClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
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
import doodler.files.StateFile
import doodler.files.StateFileList
import doodler.files.stateFileOf
import doodler.files.toStateFileList
import doodler.logger.DoodlerLogger
import java.io.File


@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun BoxScope.Selector(onSelect: (File) -> Unit = { }, validate: (File) -> Boolean = { true }) {
    DoodlerLogger.recomposition("Selector")

    val basePath by remember { mutableStateOf(System.getProperty("user.home")) }
    var value by remember { mutableStateOf(TextFieldValue("/", selection = TextRange(1))) }

    var ctrl by remember { mutableStateOf(false) }
    var shift by remember { mutableStateOf(false) }

    var candidateParentFile by remember { mutableStateOf(File("$basePath${value.text}")) }
    val candidateFiles = remember(candidateParentFile, value.text, basePath) {
        candidateParentFile.listFiles().toList()
            .filter { file -> file.absolutePath.contains("$basePath${value.text}") }
            .sortedBy { file -> file.name }
            .sortedBy { file -> if (file.isDirectory && !file.isFile) -1 else if (file.isFile) 1 else 2 }
            .map { stateFileOf(it.name, it.absolutePath, it.isDirectory, it.isFile) }
            .toStateFileList()
    }

    var haveToShift by remember { mutableStateOf(false) }
    var completeTargetFile by remember(candidateFiles) {
        val newState = if (candidateFiles.items.size == 1) candidateFiles.items[0] else null
        if (newState != null) haveToShift = true
        mutableStateOf(newState)
    }
    val completingText by remember(completeTargetFile, value.text, haveToShift) {
        if (completeTargetFile == null) return@remember mutableStateOf("")

        val enteringFileName = value.text.substring(value.text.lastIndexOf('/') + 1, value.text.length)
        val lastFileName = completeTargetFile!!.name

        val remainingFileName = lastFileName.substring(enteringFileName.length until lastFileName.length)

        mutableStateOf(remainingFileName)
    }

    val selected by remember(basePath, value.text) {
        mutableStateOf(File("$basePath${value.text}").let { file -> if (file.exists()) file else null })
    }

    val selectable by remember(selected) { mutableStateOf(selected.let { if (it == null) false else validate(it) }) }

    val displayValue by remember(value, completingText, haveToShift) {
        val newString = "${value.text}${completingText}"
        mutableStateOf(TextFieldValue(
            AnnotatedString(
                newString,
                listOf(AnnotatedString.Range(
                    SpanStyle(color = ThemedColor.from(ThemedColor.Editor.Tag.General, alpha = 80)),
                    value.text.length,
                    newString.length
                ))
            ),
            selection = if (!haveToShift) value.selection else TextRange(value.selection.start + completingText.length)
        ))
    }

    val requester by remember { mutableStateOf(FocusRequester()) }

    val remap: (TextFieldValue) -> TextFieldValue = remap@ {
        val newString = it.text.removeSuffix(completingText)
        TextFieldValue(
            newString,
            selection = TextRange(
                it.selection.start.coerceAtMost(newString.length),
                it.selection.end.coerceAtMost(newString.length)
            )
        )
    }

    val updateCandidateParent = updateCandidateParent@ {
        val path = "$basePath${value.text}"
        if (path.endsWith(".")) return@updateCandidateParent

        val newAutoCompleteFile = File(path.let { str -> str.substring(0, str.lastIndexOf('/')) })
        if (newAutoCompleteFile.exists()) candidateParentFile = newAutoCompleteFile
    }

    val onTextValueUpdated: (TextFieldValue) -> Unit = onTextValueUpdated@ {
        if (it.text.isEmpty()) return@onTextValueUpdated
        value = remap(it)
        updateCandidateParent()
    }

    val autoComplete = autoComplete@ {
        if (completeTargetFile == null) return@autoComplete false
        val newValue = "${value.text}${completingText}${if (completeTargetFile!!.isDirectory) "/" else ""}"
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
            if (completeTargetFile != null && candidateFiles.items.size == 1) {
                if (shift) return@onKeyEvent true
                autoComplete()
            } else {
                requester.requestFocus()
                if (candidateFiles.items.isEmpty()) return@onKeyEvent true
                val noneSelected = completeTargetFile == null
                val lastFile = completeTargetFile?.absolutePath == candidateFiles.items.last().absolutePath
                val firstFile = completeTargetFile?.absolutePath == candidateFiles.items.first().absolutePath
                completeTargetFile =
                    if (!shift) {
                        if (noneSelected || lastFile) candidateFiles.items.first()
                        else candidateFiles.items[candidateFiles.items.indexOf(completeTargetFile) + 1]
                    } else {
                        if (noneSelected || firstFile) candidateFiles.items.last()
                        else candidateFiles.items[candidateFiles.items.indexOf(completeTargetFile) - 1]
                    }
                haveToShift = true
            }
        } else if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
            if (!autoComplete() && selected != null && selectable && ctrl) onSelect(selected!!)
        } else if (it.key == Key.CtrlLeft || it.key == Key.CtrlRight) {
            ctrl = it.type == KeyEventType.KeyDown
        } else if (it.key == Key.ShiftLeft || it.key == Key.ShiftRight) {
            shift = it.type == KeyEventType.KeyDown
        }
        true
    }

    SideEffect {
        requester.requestFocus()
        if (haveToShift) haveToShift = false
    }

    SelectorRoot {
        PaddedContent {
            DocumentationDescription("/** you can use '..' to go parent directory */")
            PropertyDescription("basePath", basePath)
        }
        PathInputBox {
            PathInput(
                value = displayValue,
                onValueChange = onTextValueUpdated,
                onKeyEvent = onKeyEvent,
                hideCursor = haveToShift,
                focusRequester = requester
            )
            SelectButton(selectable) { onSelect(selected!!) }
        }
        PaddedContent {
            if (!candidateParentFile.isDirectory) return@PaddedContent

            Spacer(modifier = Modifier.height(15.dp))

            CandidateFiles(
                candidateFiles,
                completeTargetFile?.let { if (it.isDirectory && !it.isFile) it else null },
                "directories",
                Color(0xFFFFC66D)
            ) { it.isDirectory && !it.isFile }

            CandidateFiles(
                candidateFiles,
                completeTargetFile?.let { if (!it.isDirectory) it else null },
                "files",
                ThemedColor.Editor.Tag.General
            ) { !it.isDirectory }
        }
    }
}

@Composable
fun ColumnScope.CandidateFiles(
    targets: StateFileList,
    completeTarget: StateFile?,
    type: String,
    color: Color,
    filter: (StateFile) -> Boolean
) {
    DoodlerLogger.recomposition("CandidateFiles")

    val columns: Int by remember { derivedStateOf { 4 } }

    val filteredTargets = remember(targets) { targets.items.filter(filter).toMutableStateList() }

    val calculateRange: SnapshotStateList<StateFile>.() -> IntRange = {
        this.indexOf(filteredTargets.find { it == completeTarget })
            .coerceAtLeast(0)
            .mod(columns).minus(1)
            .coerceIn(0, (filteredTargets.size - 3).coerceAtLeast(0))
            .times(columns)
            .let { it until ((it + 3) * columns).coerceAtMost(size) }
    }

    val calculateRemains: (
        SnapshotStateList<StateFile>,
        SnapshotStateList<StateFile>
    ) -> Int = { list, subList ->
        list.size - subList.size - list.indexOf(subList.firstOrNull() ?: 0)
    }

    val printTargets by remember(filteredTargets, completeTarget) {
        derivedStateOf { filteredTargets.slice(filteredTargets.calculateRange()).toMutableStateList() }
    }

    val remainingCount by remember(filteredTargets, printTargets) {
        mutableStateOf(calculateRemains(filteredTargets, printTargets))
    }

    val adjustedColumns = listOf(0, 0, 1, 0)

    if (printTargets.isEmpty()) return

    for (chunked in printTargets.chunked(4)) {
        Row {
            for (file in chunked) {
                CandidateText(
                    file.name,
                    color = ThemedColor.from(color, alpha = 255),
                    file == completeTarget && printTargets.size != 1
                )
                Spacer(modifier = Modifier.width(15.dp))
            }
            for (dummy in 0 until adjustedColumns[chunked.size - 1]) {
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(15.dp))
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
    }
    if (remainingCount > 0) RemainingItems(remainingCount, ThemedColor.from(color, alpha = 144), type)

    Spacer(modifier = Modifier.height(25.dp))
}


@Composable
fun RowScope.CandidateText(text: String, color: Color, focused: Boolean) {
    DoodlerLogger.recomposition("CandidateText")

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
fun ColumnScope.RemainingItems(remaining: Int, color: Color, type: String) {
    DoodlerLogger.recomposition("RemainingItems")

    Text(
        "...$remaining $type more",
        color = color,
        fontFamily = JetBrainsMono,
        fontSize = 18.sp,
        maxLines = 1
    )
}

@Composable
fun BoxScope.SelectorRoot(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(top = 31.dp).requiredWidthIn(min = 790.dp), content = content)
}

@Composable
fun ColumnScope.PaddedContent(content: @Composable ColumnScope.() -> Unit) {
    Column (modifier = Modifier.padding(start = 15.dp, end = 15.dp), content = content)
}

@Composable
fun ColumnScope.DocumentationDescription(text: String) {
    Text(
        text,
        color = Color(0xFF629755),
        fontFamily = JetBrainsMono,
        fontSize = 18.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun ColumnScope.PropertyDescription(key: String, value: String) {
    Text(
        AnnotatedString(
            text = "$key = $value",
            spanStyles = listOf(
                AnnotatedString.Range(SpanStyle(color = ThemedColor.ChunkSelectorPropertyKey), 0, key.length + 2)
            )
        ),
        color = ThemedColor.Editor.Tag.General,
        fontFamily = JetBrainsMono,
        fontSize = 22.sp,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
fun ColumnScope.PathInputBox(content: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().background(Color(32, 32, 32), RoundedCornerShape(5.dp)).height(60.dp),
        content = content
    )
}

@Composable
fun RowScope.PathInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onKeyEvent: (KeyEvent) -> Boolean,
    hideCursor: Boolean,
    focusRequester: FocusRequester
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            color = ThemedColor.Editor.Tag.General,
            fontSize = 23.sp,
            fontFamily = JetBrainsMono
        ),
        cursorBrush = if (!hideCursor) SolidColor(ThemedColor.Editor.Tag.General) else SolidColor(Color.Transparent),
        modifier = Modifier.weight(1f)
            .onKeyEvent(onKeyEvent)
            .focusRequester(focusRequester)
            .padding(start = 15.dp, end = 15.dp),
        singleLine = true
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowScope.SelectButton(
    enabled: Boolean,
    onSelect: () -> Unit
) {
    Box (
        modifier = Modifier.width(80.dp).fillMaxHeight().padding(5.dp)
            .mouseClickable {
                if (!enabled) return@mouseClickable
                if (buttons.isPrimaryPressed) onSelect()
            }
            .alpha(if (enabled) 1.0f else 0.6f)
    ) {
        Box (modifier = Modifier.background(Color(0xff55783d), RoundedCornerShape(5.dp)).fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Go!", fontFamily = JetBrainsMono, color = Color(0xFFCCCCCC), fontSize = 22.sp)
        }
    }
}
