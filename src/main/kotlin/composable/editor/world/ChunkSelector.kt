package composable.editor.world

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.zIndex
import doodler.editor.GlobalMcaEditor
import doodler.editor.McaEditor
import doodler.editor.McaPayload
import doodler.editor.states.McaEditorState
import doodler.minecraft.structures.AnvilLocationSurroundings
import doodler.minecraft.structures.BlockLocation
import doodler.minecraft.structures.ChunkLocation
import doodler.theme.DoodlerTheme
import doodler.unit.ScaledUnits.ChunkSelector.Companion.scaled
import doodler.unit.dp
import java.io.File

@Composable
fun ChunkSelector(
    editor: McaEditor<*>,
    chunks: List<ChunkLocation>,
    update: (McaPayload) -> Unit,
    openChunkNbt: (ChunkLocation, File) -> Unit,
    defaultStateProvider: () -> McaEditorState
) {

    val state = editor.state(defaultStateProvider)
    val payload = editor.payload

    val currentAnvil by remember(payload.location) {
        derivedStateOf { state.selectedChunk?.toAnvilLocation() ?: payload.location }
    }

    val availableAnvils by remember(chunks) {
        derivedStateOf { chunks.map { it.toAnvilLocation() }.toSet().sortedBy { "${it.x}.${it.z}" } }
    }

    val surroundingAnvils by remember {
        derivedStateOf { AnvilLocationSurroundings.fromBase(currentAnvil, availableAnvils) }
    }

    val existingAnvil = {
        availableAnvils.contains(currentAnvil)
    }

    val chunkOrNull = {
        val xInt = state.chunkXValue.text.toIntOrNull()
        val zInt = state.chunkZValue.text.toIntOrNull()

        if (xInt == null || zInt == null) null
        else {
            val chunk = ChunkLocation(xInt, zInt)
            if (!chunks.contains(chunk)) null
            else chunk
        }
    }

    val blockOrNull = {
        val xInt = state.blockXValue.text.toIntOrNull()
        val zInt = state.blockZValue.text.toIntOrNull()

        if (xInt == null || zInt == null) null
        else {
            val block = BlockLocation(xInt, zInt)
            if (!chunks.contains(block.toChunkLocation())) null
            else block
        }
    }

    val resetBlock = {
        state.blockXValue = TextFieldValue("-")
        state.blockZValue = TextFieldValue("-")
    }

    val resetChunk = {
        state.selectedChunk = null
        state.chunkXValue = TextFieldValue("-")
        state.chunkZValue = TextFieldValue("-")
        resetBlock()
    }

    val chunkUpdated = update@ {
        resetBlock()

        val prevState = state.selectedChunk
        val newState = chunkOrNull()

        if (prevState == newState) return@update
        state.selectedChunk = newState
    }

    val blockUpdated = update@ {
        val block = blockOrNull() ?: return@update
        val chunk = block.toChunkLocation()

        state.chunkXValue = TextFieldValue("${chunk.x}")
        state.chunkZValue = TextFieldValue("${chunk.x}")
    }

    val openPopup: (String) -> Unit = {

    }

    val enabled = true

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(DoodlerTheme.Colors.Editor.McaEditorDropdownBackground)
            .height(30.dp)
            .fillMaxWidth()
            .zIndex(10f)
    ) {
        DropdownSpacer()
        Dropdown(prefix = "block:", enabled = enabled) {
            Coordinate(
                xValue = state.blockXValue, zValue = state.blockZValue,
                onXChange = { state.blockXValue = it; blockUpdated() },
                onZChange = { state.blockZValue = it; blockUpdated() },
                transformer = transformBlockCoordinate,
                enabled = enabled
            )
        }
        DropdownSpacer()
        Dropdown(prefix = "chunk:", accent = true, valid = state.selectedChunk != null, enabled = enabled) {
            Coordinate(
                xValue = state.chunkXValue, zValue = state.chunkZValue,
                onXChange = { state.chunkXValue = it; chunkUpdated(); },
                onZChange = { state.chunkZValue = it; chunkUpdated(); },
                xTransformer = { text -> transformChunkCoordinate(text) { chunks.hasX(it) } },
                zTransformer = { text -> transformChunkCoordinate(text) { chunks.hasZ(it) } },
                enabled = enabled
            )
        }
        DropdownSpacer()
        Dropdown(
            prefix = "region:",
            onClick = if (availableAnvils.size != 1) ({ openPopup("region") }) else null,
            modifier = Modifier
        ) {
            val chunk = chunkOrNull() != null
            val region = existingAnvil()
            val x = "${currentAnvil.x}"
            val z = "${currentAnvil.z}"
            CoordinateText(
                AnnotatedString(
                    text = "r.$x.$z.mca",
                    spanStyles = listOf(
                        AnnotatedString.Range(
                            SpanStyle(color = DoodlerTheme.Colors.Text.NumberColor(chunk && region)),
                            start = 2,
                            end = 2 + x.length
                        ),
                        AnnotatedString.Range(
                            SpanStyle(color = DoodlerTheme.Colors.Text.NumberColor(chunk && region)),
                            start = 2 + x.length + 1,
                            end = 2 + x.length + 1 + z.length
                        )
                    )
                )
            )
        }
        DropdownSpacer()
        Dropdown(
            prefix = "type:",
            onClick = if (editor is GlobalMcaEditor) ({ openPopup("type") }) else null,
            modifier = Modifier
        ) {
            CoordinateText(payload.type.name)
        }
        DropdownSpacer()
        Dropdown(
            prefix = "dim:",
            onClick = if (editor is GlobalMcaEditor) ({ openPopup("dimension") }) else null,
            modifier = Modifier
        ) {
            CoordinateText(payload.dimension.name)
        }
    }

}

@Composable
fun Dropdown(
    prefix: String,
    accent: Boolean = false,
    valid: Boolean = true,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val hoverInteraction = remember { MutableInteractionSource() }
    val hovered by hoverInteraction.collectIsHoveredAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.then(modifier)
            .drawBehind {
                drawRoundRect(
                    DoodlerTheme.Colors.Editor.DropdownBackground(
                        accent = accent,
                        valid = valid,
                        hovered = hovered
                    ).copy(alpha = if (enabled) 1f else 0.6f),
                    cornerRadius = CornerRadius(2.dp.value)
                )
            }
            .height(20.dp)
            .alpha(if (enabled) 1f else 0.6f)
            .let {
                if (onClick != null) it.hoverable(hoverInteraction).clickable(onClick = onClick)
                else it
            }
    ) {
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = prefix,
            fontSize = MaterialTheme.typography.h5.fontSize.scaled,
            color = DoodlerTheme.Colors.Text.Normal
        )
        Spacer(modifier = Modifier.width(1.dp))
        content()
        Spacer(modifier = Modifier.width(6.dp))
    }
}

@Composable
fun DropdownSpacer() = Spacer(modifier = Modifier.width(5.dp))

@Composable
fun Coordinate(
    xValue: TextFieldValue,
    zValue: TextFieldValue,
    onXChange: (TextFieldValue) -> Unit,
    onZChange: (TextFieldValue) -> Unit,
    transformer: (AnnotatedString) -> TransformedText,
    enabled: Boolean = true
) = Coordinate(xValue, zValue, onXChange, onZChange, transformer, transformer, enabled)

@Composable
fun Coordinate(
    xValue: TextFieldValue,
    zValue: TextFieldValue,
    onXChange: (TextFieldValue) -> Unit,
    onZChange: (TextFieldValue) -> Unit,
    xTransformer: (AnnotatedString) -> TransformedText,
    zTransformer: (AnnotatedString) -> TransformedText,
    enabled: Boolean = true
) {
    CoordinateText("[")
    CoordinateInput(
        value = xValue,
        onValueChange = onXChange,
        transformer = xTransformer,
        enabled = enabled
    )
    CoordinateText(", ")
    CoordinateInput(
        value = zValue,
        onValueChange = onZChange,
        transformer = zTransformer,
        enabled = enabled
    )
    CoordinateText("]")
}

@Composable
fun CoordinateText(
    text: String, valid: Boolean = true
) = Text(
    text = text,
    fontSize = MaterialTheme.typography.h4.fontSize.scaled,
    color = if (valid) DoodlerTheme.Colors.Text.IdeGeneral else DoodlerTheme.Colors.Text.Invalid,
    modifier = Modifier.focusable(false)
)

@Composable
fun CoordinateText(
    text: AnnotatedString
) = Text(
    text = text,
    fontSize = MaterialTheme.typography.h4.fontSize.scaled,
    color = DoodlerTheme.Colors.Text.IdeGeneral,
    modifier = Modifier.focusable(false)
)

@Composable
fun CoordinateInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    transformer: (AnnotatedString) -> TransformedText,
    enabled: Boolean = true
) = BasicTextField(
    value = value,
    onValueChange = onValueChange,
    textStyle = TextStyle(
        fontSize = MaterialTheme.typography.h4.fontSize.scaled,
        color = DoodlerTheme.Colors.Text.IdeGeneral,
        fontFamily = DoodlerTheme.Fonts.JetbrainsMono
    ),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    singleLine = true,
    cursorBrush = SolidColor(DoodlerTheme.Colors.Text.IdeGeneral),
    visualTransformation = transformer,
    enabled = enabled,
    modifier = Modifier
        .width((value.text.length.coerceAtLeast(1) * 9.140625f).dp.scaled)
        .focusable(false)
        .onFocusChanged {
            if (!it.isFocused && value.text.isEmpty()) onValueChange(TextFieldValue("-"))
        }
)

private fun List<ChunkLocation>.hasX(x: Int) = map { it.x }.contains(x)
private fun List<ChunkLocation>.hasZ(z: Int) = map { it.z }.contains(z)

val transformBlockCoordinate: (AnnotatedString) -> TransformedText = { annotated ->
    val int = annotated.text.toIntOrNull()

    val color =
        if (int == null) DoodlerTheme.Colors.Text.Malformed
        else DoodlerTheme.Colors.Text.IdeGeneral

    val spans = listOf(AnnotatedString.Range(SpanStyle(color = color), 0, annotated.text.length))

    TransformedText(AnnotatedString(annotated.text, spans), OffsetMapping.Identity)
}

val transformChunkCoordinate: (AnnotatedString, (Int) -> Boolean) -> TransformedText = validate@ { annotated, criteria ->
    val int = annotated.text.toIntOrNull()

    val color =
        if (int == null) DoodlerTheme.Colors.Text.Malformed
        else if (!criteria(int)) DoodlerTheme.Colors.Text.Invalid
        else DoodlerTheme.Colors.Text.IdeGeneral

    val spans = listOf(AnnotatedString.Range(SpanStyle(color = color), 0, annotated.text.length))

    TransformedText(AnnotatedString(annotated.text, spans), OffsetMapping.Identity)
}
