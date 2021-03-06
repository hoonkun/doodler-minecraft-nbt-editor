package composable.selector

import doodler.minecraft.DatWorker
import doodler.nbt.tag.CompoundTag
import doodler.nbt.tag.StringTag
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import doodler.application.structure.DoodlerEditorType
import doodler.file.StateFile
import doodler.file.StateFileList
import doodler.file.toStateFile
import doodler.file.toStateFileList
import doodler.theme.DoodlerTheme
import doodler.unit.ddp
import doodler.unit.dsp
import java.io.File


private val BasePath = System.getProperty("user.home")

private const val Columns = 4

private val File.typeId get() = if (isDirectory && !isFile) -1 else if (isFile) 1 else 2

private fun File.validateAs(type: DoodlerEditorType): File? {
    when(type) {
        DoodlerEditorType.World -> {
            if (File("${this.absolutePath}/server.properties").exists()) return this

            val level = File("${this.absolutePath}/level.dat")
            if (!level.exists()) return null

            DatWorker.read(level.readBytes())["Data"]
                ?.getAs<CompoundTag>()
                ?.get("LevelName")
                ?.getAs<StringTag>()
                ?.value
                ?: return null

            return this
        }
        DoodlerEditorType.Standalone -> {
            return try {
                DatWorker.read(this.readBytes())
                this
            } catch (e: Exception) { null }
        }
    }
}

private fun String.removeSuffixes(suffix: CharSequence): String {
    var result = this
    while (result.endsWith(suffix)) result = result.removeSuffix(suffix)
    return result
}

private fun StateFileList.printTargets(hintTarget: StateFile?): StateFileList {
    val range = items.indexOf(items.find { it == hintTarget }).coerceAtLeast(0)
        .div(Columns).minus(1).coerceAtLeast(0).times(Columns)
        .let {
            val minFirstRow = ((items.size + items.size.mod(Columns)).div(Columns) - 3).coerceAtLeast(0)
            val minInclusive = it.coerceAtMost(minFirstRow * Columns)
            val maxExclusive = (minInclusive + 3 * Columns).coerceAtMost(items.size)
            minInclusive until maxExclusive
        }
    return items.slice(range).toStateFileList()
}

private fun remaining(entire: StateFileList, printTargets: StateFileList): Int =
    entire.items.size - printTargets.items.size - entire.items.indexOf(printTargets.items.firstOrNull() ?: 0)

data class KeySet(
    var ctrl: Boolean,
    var shift: Boolean
)

enum class CandidateType(
    val displayName: String,
    val color: Color,
    val criteria: (StateFile) -> Boolean
) {
    Directory("directories", DoodlerTheme.Colors.Selector.Directories, { it.isDirectory && !it.isFile }),
    File("files", DoodlerTheme.Colors.Selector.Files, { !it.isDirectory })
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Selector(targetType: DoodlerEditorType, onSelect: (File, DoodlerEditorType) -> Unit) {

    var path by remember { mutableStateOf(TextFieldValue("/", selection = TextRange(1))) }
    val entirePath by remember(path.text) { derivedStateOf { "$BasePath${path.text}" } }

    val keys = remember { KeySet(ctrl = false, shift = false) }

    var haveToShiftField by remember { mutableStateOf(false) }

    val candidateTarget = remember(entirePath) {
        File(entirePath.removeSuffixes(".").substring(0, entirePath.lastIndexOf('/')))
    }
    val candidates = remember(candidateTarget, entirePath) {
        (candidateTarget.listFiles() ?: arrayOf())
            .filter { file -> file.absolutePath.contains(entirePath) }
            .sortedWith(compareBy({ it.typeId }, { it.name }))
            .map { it.toStateFile() }
            .toStateFileList()
    }

    var hintTarget by remember(candidates) {
        val newState = if (candidates.items.size == 1) candidates.items.first() else null
        if (newState != null) haveToShiftField = true

        mutableStateOf(newState)
    }
    val hint = remember(path.text, hintTarget) {
        val target = hintTarget ?: return@remember ""

        val entered = path.text.let { it.substring(it.lastIndexOf('/') + 1, it.length) }
        val entire = target.name

        entire.substring(entered.length until entire.length)
    }

    val value = remember(path.text, haveToShiftField, hint) {
        val newText = "${path.text}${hint}"
        val annotatedText = AnnotatedString(
            text = newText,
            listOf(AnnotatedString.Range(
                item = SpanStyle(color = DoodlerTheme.Colors.OnBackground.copy(alpha = 0.313f)),
                start = path.text.length,
                end = newText.length
            ))
        )
        val selection =
            if (haveToShiftField) TextRange(path.selection.start + hint.length)
            else path.selection

        TextFieldValue(annotatedText, selection)
    }

    val selected = remember(path.text, targetType) {
        File("$BasePath${path.text}").validateAs(targetType)
    }

    val requester = remember { FocusRequester() }

    val value2path: (TextFieldValue) -> TextFieldValue = transform@ {
        val newPath = it.text.removeSuffix(hint)

        TextFieldValue(
            text = newPath,
            selection = TextRange(
                it.selection.start.coerceAtMost(newPath.length),
                it.selection.end.coerceAtMost(newPath.length)
            )
        )
    }

    val complete = complete@ {
        val target = hintTarget ?: return@complete false
        val newPath = "${path.text}$hint${if (target.isDirectory) "/"  else ""}"
        path = TextFieldValue(text = newPath, selection = TextRange(newPath.length))
        hintTarget = null
        true
    }

    val onKeyEvent: (KeyEvent) -> Unit = {
        if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
            if (hintTarget != null && candidates.items.size == 1) {
                if (!keys.shift) complete()
            } else if (candidates.items.isNotEmpty()) {
                val noneSelected = hintTarget == null
                val lastFile = hintTarget?.absolutePath == candidates.items.last().absolutePath
                val firstFile = hintTarget?.absolutePath == candidates.items.first().absolutePath
                hintTarget =
                    if (!keys.shift) {
                        if (noneSelected || lastFile) candidates.items.first()
                        else candidates.items[candidates.items.indexOf(hintTarget) + 1]
                    } else {
                        if (noneSelected || firstFile) candidates.items.last()
                        else candidates.items[candidates.items.indexOf(hintTarget) - 1]
                    }
                haveToShiftField = true
            }
            requester.requestFocus()
        } else if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
            if (!complete() && selected != null && keys.ctrl) onSelect(selected, targetType)
        } else if (it.key == Key.CtrlLeft || it.key == Key.CtrlRight) {
            keys.ctrl = it.type == KeyEventType.KeyDown
        } else if (it.key == Key.ShiftLeft || it.key == Key.ShiftRight) {
            keys.shift = it.type == KeyEventType.KeyDown
        }
    }

    SideEffect {
        requester.requestFocus()
        if (haveToShiftField) haveToShiftField = false
    }

    SelectorRoot {
        Padded {
            BasePathDocumentation(text = "/** you can use '..' to go parent directory */")
            BasePathProperty(key = "basePath", value = BasePath)
        }
        PathInputBox {
            PathInput(
                value = value,
                onValueChange = { if (it.text.isNotEmpty()) path = value2path(it) },
                onKeyEvent = onKeyEvent,
                hideCursor = haveToShiftField,
                focusRequester = requester
            )
            Select(enabled = selected != null) { selected?.let { onSelect(it, targetType) } }
        }
        Padded {
            if (!candidateTarget.isDirectory) return@Padded

            Candidates(
                type = CandidateType.Directory,
                candidates = candidates,
                hintTarget = hintTarget
            )

            Candidates(
                type = CandidateType.File,
                candidates = candidates,
                hintTarget = hintTarget
            )
        }
    }

}

@Composable
fun ColumnScope.Candidates(
    type: CandidateType,
    candidates: StateFileList,
    hintTarget: StateFile?
) {
    val typed = remember(candidates, type.criteria) { candidates.items.filter(type.criteria).toStateFileList() }
    val printTargets = remember(typed, hintTarget) { typed.printTargets(hintTarget) }
    val remaining = remember(typed, printTargets) { remaining(typed, printTargets) }

    if (printTargets.items.isEmpty()) return

    for (chunked in printTargets.items.chunked(Columns)) {
        Row {
            for (candidate in chunked) {
                Candidate(type, candidate, candidate == hintTarget && printTargets.items.size != 1)
            }
            if (chunked.size == 3) {
                Spacer(modifier = Modifier.weight(1f).padding(end = 15.ddp, bottom = 5.ddp))
            }
        }
    }
    if (remaining > 0) Remaining(remaining, type)

    Spacer(modifier = Modifier.height(15.ddp))
}

@Composable
fun RowScope.Candidate(
    type: CandidateType,
    candidate: StateFile,
    focused: Boolean
) = Box(modifier = Modifier.weight(1f).padding(end = 15.ddp, bottom = 5.ddp)) {
    Text(
        text = candidate.name,
        color = type.color,
        fontSize = 10.dsp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.drawBehind { if (focused) drawRect(DoodlerTheme.Colors.Selector.FocusedCandidate) }
    )
}

@Composable
fun ColumnScope.Remaining(
    count: Int,
    type: CandidateType
) = Text(
    "...$count ${type.displayName} more",
    color = type.color,
    fontSize = 10.dsp,
    maxLines = 1
)

@Composable
fun SelectorRoot(
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .requiredWidthIn(min = 500.ddp)
            .background(DoodlerTheme.Colors.Background)
            .padding(start = 12.5f.ddp, end = 12.5f.ddp, top = 16.ddp)
    ) {
        Column(
            content = content
        )
    }
}

@Composable
fun ColumnScope.Padded(
    top: Dp = 0.ddp,
    content: @Composable ColumnScope.() -> Unit
) = Column(modifier = Modifier.padding(start = 15.ddp, end = 15.ddp, top = top), content = content)

@Composable
fun ColumnScope.BasePathDocumentation(text: String) =
    Text(
        text = text,
        color = DoodlerTheme.Colors.Text.IdeDocumentation,
        fontFamily = DoodlerTheme.Fonts.JetbrainsMono,
        fontSize = 10.dsp,
        modifier = Modifier.padding(bottom = 8.ddp)
    )

@Composable
fun ColumnScope.BasePathProperty(key: String, value: String) =
    Text(
        AnnotatedString(
            text = "$key = $value",
            spanStyles = listOf(
                AnnotatedString.Range(
                    item = SpanStyle(color = DoodlerTheme.Colors.Text.IdeFunctionProperty),
                    start = 0,
                    end = key.length + 2
                )
            )
        ),
        color = DoodlerTheme.Colors.Text.IdeGeneral,
        fontSize = 12.dsp,
        modifier = Modifier.padding(bottom = 1.ddp)
    )

@Composable
fun ColumnScope.PathInputBox(content: @Composable RowScope.() -> Unit) =
    Box(
        modifier = Modifier.padding(vertical = 10.ddp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(35.ddp)
                .background(DoodlerTheme.Colors.Selector.PathInput, RoundedCornerShape(5.ddp)),
            content = content
        )
    }

@Composable
fun RowScope.PathInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onKeyEvent: (KeyEvent) -> Unit,
    hideCursor: Boolean,
    focusRequester: FocusRequester
) = BasicTextField(
    value = value,
    onValueChange = onValueChange,
    textStyle = TextStyle(
        color = DoodlerTheme.Colors.Text.IdeGeneral,
        fontSize = 12.dsp,
        fontFamily = DoodlerTheme.Fonts.JetbrainsMono
    ),
    cursorBrush =
        if (hideCursor) SolidColor(Color.Transparent)
        else SolidColor(DoodlerTheme.Colors.Text.IdeGeneral),
    singleLine = true,
    modifier = Modifier.weight(1f)
        .onKeyEvent { onKeyEvent(it); true }
        .focusRequester(focusRequester)
        .padding(start = 10.ddp, end = 10.ddp)
)

@Composable
fun RowScope.Select(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        modifier = Modifier.width(60.ddp).fillMaxHeight().padding(5.ddp)
            .clickable { if (enabled) onClick() }
            .alpha(if (enabled) 1f else 0.6f)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .drawBehind {
                    drawRoundRect(
                        color =
                            if (enabled && hovered) DoodlerTheme.Colors.Selector.SelectButtonHovered
                            else DoodlerTheme.Colors.Selector.SelectButton,
                        cornerRadius = CornerRadius(5.ddp.value, 5.ddp.value)
                    )
                }
                .fillMaxSize()
                .hoverable(interaction)
        ) {
            Text("Go!", color = Color(0xffcccccc), fontSize = 12.dsp)
        }
    }
}
