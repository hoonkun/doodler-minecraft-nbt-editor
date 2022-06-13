package composable.editor.world

import activator.composables.global.ThemedColor
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import doodler.editor.GlobalMcaEditor
import doodler.editor.McaEditor
import doodler.editor.McaPayload
import doodler.editor.TerrainCache
import doodler.editor.states.McaEditorState
import doodler.minecraft.structures.*
import doodler.theme.DoodlerTheme
import doodler.unit.adp
import doodler.unit.ddp
import java.io.File


private val TextStyle.fsp get() = this.fontSize * 0.6f

@Composable
fun ChunkSelector(
    editor: McaEditor<*>,
    terrains: List<File>,
    chunks: List<ChunkLocation>,
    terrainCache: TerrainCache,
    update: (McaPayload) -> Unit,
    openChunkNbt: (ChunkLocation, File) -> Unit,
    defaultStateProvider: () -> McaEditorState
) {

    val state = editor.state(defaultStateProvider)
    val payload = editor.payload

    val currentAnvil = remember(state.selectedChunk, payload.location) {
        state.selectedChunk?.toAnvilLocation() ?: payload.location
    }

    val availableAnvils = remember(chunks) {
        chunks.map { it.toAnvilLocation() }.toSet().sortedBy { "${it.x}.${it.z}" }
    }

    val surroundingAnvils by remember(currentAnvil, availableAnvils) {
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

    val setSelectedChunk: (ChunkLocation?) -> Unit = { chunk ->
        state.selectedChunk = chunk
        chunk?.toAnvilLocation()?.let {
            update(payload.copy(location = it, file = siblingAnvil(payload.file, it)))
        }
    }

    val chunkUpdated = update@ {
        val prevState = state.selectedChunk
        val newState = chunkOrNull()

        if (prevState == newState) return@update

        resetBlock()
        setSelectedChunk(newState)
    }

    val blockUpdated = update@ {
        val block = blockOrNull() ?: return@update
        val chunk = block.toChunkLocation()

        state.chunkXValue = TextFieldValue("${chunk.x}")
        state.chunkZValue = TextFieldValue("${chunk.z}")
        setSelectedChunk(chunk)
    }

    var expandedDropdown by remember { mutableStateOf<String?>(null) }

    val selectorItemPositions = remember { mutableMapOf<String, Offset>() }

    val closeDropdown: () -> Unit = { expandedDropdown = null }

    val openDropdown: (String) -> Unit = {
        if (expandedDropdown == it) closeDropdown()
        else expandedDropdown = it
    }

    val onItemPositioned: LayoutCoordinates.(String) -> Unit = { selectorItemPositions[it] = positionInParent() }

    val coordinateEnabled by remember { derivedStateOf { expandedDropdown == null } }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(DoodlerTheme.Colors.Editor.McaEditorDropdownBackground)
            .height(30.ddp)
            .fillMaxWidth()
            .zIndex(10f)
    ) {
        SelectorItemSpacer()
        SelectorItem(ident = "block", enabled = coordinateEnabled) {
            Coordinate(
                xValue = state.blockXValue, zValue = state.blockZValue,
                onXChange = { state.blockXValue = it; blockUpdated() },
                onZChange = { state.blockZValue = it; blockUpdated() },
                transformer = transformBlockCoordinate,
                enabled = coordinateEnabled
            )
        }
        SelectorItemSpacer()
        SelectorItem(
            ident = "chunk",
            accent = true,
            valid = state.selectedChunk != null,
            enabled = coordinateEnabled
        ) {
            Coordinate(
                xValue = state.chunkXValue, zValue = state.chunkZValue,
                onXChange = { state.chunkXValue = it; chunkUpdated(); },
                onZChange = { state.chunkZValue = it; chunkUpdated(); },
                xTransformer = { text -> transformChunkCoordinate(text) { chunks.hasX(it) } },
                zTransformer = { text -> transformChunkCoordinate(text) { chunks.hasZ(it) } },
                enabled = coordinateEnabled
            )
        }
        SelectorItemSpacer()
        SelectorItem(
            ident = "region",
            onClick = if (availableAnvils.any { it != currentAnvil }) ({ openDropdown(it) }) else null,
            onGloballyPositioned = onItemPositioned
        ) {
            val region = existingAnvil()
            val x = "${currentAnvil.x}"
            val z = "${currentAnvil.z}"
            CoordinateText(
                AnnotatedString(
                    text = "r.$x.$z.mca",
                    spanStyles = listOf(
                        AnnotatedString.Range(
                            SpanStyle(color = DoodlerTheme.Colors.Text.NumberColor(region)),
                            start = 2,
                            end = 2 + x.length
                        ),
                        AnnotatedString.Range(
                            SpanStyle(color = DoodlerTheme.Colors.Text.NumberColor(region)),
                            start = 2 + x.length + 1,
                            end = 2 + x.length + 1 + z.length
                        )
                    )
                )
            )
        }
        SelectorItemSpacer()
        SelectorItem(
            ident = "type",
            onClick = if (editor is GlobalMcaEditor) ({ openDropdown(it) }) else null,
            onGloballyPositioned = onItemPositioned
        ) {
            CoordinateText(payload.type.name)
        }
        SelectorItemSpacer()
        SelectorItem(
            ident = "dim",
            onClick = if (editor is GlobalMcaEditor) ({ openDropdown(it) }) else null,
            onGloballyPositioned = onItemPositioned
        ) {
            CoordinateText(payload.dimension.name)
        }
        Spacer(modifier = Modifier.weight(1f))
        OpenButton(
            enabled = { state.selectedChunk != null },
            onClick = {
                val chunk = state.selectedChunk
                val file = chunk?.let { siblingAnvil(payload.file, chunk.toAnvilLocation()) }
                if (chunk != null && file != null) openChunkNbt(chunk, file)
            }
        )
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        AnvilPreview(
            terrain = terrains.find { it.name == "r.${currentAnvil.x}.${currentAnvil.z}.mca" },
            properties = ChunkPreviewProperties(payload.dimension, surroundingAnvils),
            cache = terrainCache,
            chunk = state.selectedChunk,
            anvil = currentAnvil,
            hasNbt = { chunks.contains(it) },
            moveToSurroundings = {
                resetChunk()
                update(payload.copy(location = it, file = siblingAnvil(payload.file, it)))
            },
            invalidateCache = {
                terrainCache.terrains
                    .filter { it.key.location == currentAnvil }.keys
                    .forEach { terrainCache.terrains.remove(it) }
            },
            onItemClick = onItemClick@ {
                if (!chunks.contains(it)) return@onItemClick
                state.chunkXValue = TextFieldValue("${it.x}")
                state.chunkZValue = TextFieldValue("${it.z}")
                state.selectedChunk = it
                resetBlock()
            }
        )

        DropdownBackground(
            enabled = expandedDropdown != null,
            onCloseRequest = closeDropdown
        )

        Box(contentAlignment = Alignment.TopStart, modifier = Modifier.fillMaxSize()) {
            when (expandedDropdown) {
                "region" ->
                    Dropdown(
                        ident = "region",
                        items = { availableAnvils.filter { it != currentAnvil } },
                        valueMapper = { "r.${it.x}.${it.z}.mca" },
                        anchor = selectorItemPositions.getValue("region"),
                        onCloseRequest = closeDropdown,
                        onClick = {
                            resetChunk()
                            update(payload.copy(location = it))
                        }
                    )
                "type" ->
                    Dropdown(
                        ident = "type",
                        items = { McaType.values().filter { it != payload.type } },
                        valueMapper = { it.name },
                        anchor = selectorItemPositions.getValue("type"),
                        onCloseRequest = closeDropdown,
                        onClick = {
                            resetChunk()
                            update(payload.copy(type = it))
                        }
                    )
                "dim" ->
                    Dropdown(
                        ident = "dim",
                        items = { WorldDimension.values().filter { it != payload.dimension } },
                        valueMapper = { it.name },
                        anchor = selectorItemPositions.getValue("dim"),
                        onCloseRequest = closeDropdown,
                        onClick = {
                            resetChunk()
                            update(payload.copy(dimension = it))
                        }
                    )
            }
        }
    }

}

@Composable
fun SelectorItem(
    ident: String = "UNSPECIFIED",
    accent: Boolean = false,
    valid: Boolean = true,
    enabled: Boolean = true,
    requiredWidth: Dp? = null,
    onClick: ((String) -> Unit)? = null,
    onGloballyPositioned: (LayoutCoordinates.(String) -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val hoverInteraction = remember { MutableInteractionSource() }
    val hovered by hoverInteraction.collectIsHoveredAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .drawBehind {
                drawRoundRect(
                    DoodlerTheme.Colors.Editor.DropdownBackground(
                        accent = accent,
                        valid = valid,
                        hovered = hovered
                    ).copy(alpha = if (enabled) 1f else 0.6f),
                    cornerRadius = CornerRadius(1.2.ddp.value)
                )
            }
            .height(20.ddp)
            .alpha(if (enabled) 1f else 0.6f)
            .let {
                if (onClick != null) it.hoverable(hoverInteraction).clickable(onClick = { onClick(ident) })
                else it
            }
            .let {
                if (onGloballyPositioned != null) it.onGloballyPositioned { lc -> lc.onGloballyPositioned(ident) }
                else it
            }
            .let { if (requiredWidth != null) it.width(requiredWidth) else it }
    ) {
        Spacer(modifier = Modifier.width(5.ddp))
        if (ident != "UNSPECIFIED") {
            Text(
                text = "$ident:",
                fontSize = MaterialTheme.typography.h5.fsp,
                color = DoodlerTheme.Colors.Text.Normal
            )
            Spacer(modifier = Modifier.width(0.6.ddp))
        }
        content()
        Spacer(modifier = Modifier.width(5.ddp))
    }
}

@Composable
fun SelectorItemSpacer() = Spacer(modifier = Modifier.width(5.ddp))

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
    fontSize = MaterialTheme.typography.h4.fsp,
    color = if (valid) DoodlerTheme.Colors.Text.IdeGeneral else DoodlerTheme.Colors.Text.Invalid,
    modifier = Modifier.focusable(false)
)

@Composable
fun CoordinateText(
    text: AnnotatedString
) = Text(
    text = text,
    fontSize = MaterialTheme.typography.h4.fsp,
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
        fontSize = MaterialTheme.typography.h4.fsp,
        color = DoodlerTheme.Colors.Text.IdeGeneral,
        fontFamily = DoodlerTheme.Fonts.JetbrainsMono
    ),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    singleLine = true,
    cursorBrush = SolidColor(DoodlerTheme.Colors.Text.IdeGeneral),
    visualTransformation = transformer,
    enabled = enabled,
    modifier = Modifier
        .width((value.text.length.coerceAtLeast(1) * 5.484375).ddp)
        .focusable(false)
        .onFocusChanged {
            if (!it.isFocused && value.text.isEmpty()) onValueChange(TextFieldValue("-"))
        }
)

@Composable
fun DropdownBackground(
    enabled: Boolean,
    onCloseRequest: () -> Unit
) {
    if (!enabled) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemedColor.from(Color.Black, alpha = 150))
            .clickable(onClick = onCloseRequest)
    )
}

@Composable
fun <K>Dropdown(
    ident: String,
    items: () -> List<K>,
    valueMapper: (K) -> String,
    anchor: Offset,
    onCloseRequest: () -> Unit,
    onClick: (K) -> Unit
) {
    val scrollState = rememberScrollState()
    var width by remember { mutableStateOf<Int?>(null) }

    Row {
        Text(
            text = "$ident:",
            fontSize = MaterialTheme.typography.h5.fsp,
            modifier = Modifier.alpha(0f)
        )
        Spacer(modifier = Modifier.width(0.6.ddp))
        Column(
            modifier = Modifier
                .absoluteOffset(x = anchor.x.adp)
                .padding(top = 4.2.ddp)
                .verticalScroll(scrollState)
                .onGloballyPositioned { width = it.size.width }
        ) {
            for (item in items()) {
                SelectorItem(
                    onClick = { onClick(item); onCloseRequest() },
                    requiredWidth = width?.adp
                ) {
                    CoordinateText(valueMapper(item))
                    Spacer(modifier = Modifier.width(3.ddp))
                }
                Spacer(modifier = Modifier.height(6.ddp))
            }
        }
    }
}

@Composable
fun OpenButton(
    enabled: () -> Boolean,
    onClick: () -> Unit
) {
    val hoverInteractionSource = remember { MutableInteractionSource() }
    val pressInteractionSource = remember { MutableInteractionSource() }

    val hovered by hoverInteractionSource.collectIsHoveredAsState()
    val pressed by pressInteractionSource.collectIsPressedAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .hoverable(hoverInteractionSource).clickable(pressInteractionSource, null) { onClick() }
            .width(30.ddp).height(30.ddp)
            .alpha(if (enabled()) 1f else 0.5f)
            .drawBehind {
                drawRect(
                    if (!enabled()) Color.Transparent
                    else if (pressed) Color.White.copy(alpha = 0.0352f)
                    else if (hovered) Color.White.copy(alpha = 0.0776f)
                    else Color.Transparent
                )
            }
    ) {
        Text(
            "->",
            fontSize = MaterialTheme.typography.h4.fsp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.6862f)
        )
    }
}

private fun List<ChunkLocation>.hasX(x: Int) = map { it.x }.contains(x)
private fun List<ChunkLocation>.hasZ(z: Int) = map { it.z }.contains(z)

private fun siblingAnvil(file: File, location: AnvilLocation) =
    File("${file.parentFile.absolutePath}/r.${location.x}.${location.z}.mca")

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
