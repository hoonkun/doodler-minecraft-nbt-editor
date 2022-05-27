package composables.editor.world

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import composables.stateful.editor.*
import composables.global.JetBrainsMono
import composables.global.ThemedColor
import doodler.editor.McaEditor
import doodler.editor.McaPayload
import doodler.editor.states.SelectorState
import doodler.minecraft.structures.*
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.ChunkSelector(
    chunks: List<ChunkLocation>,
    tree: WorldHierarchy,
    selector: McaEditor,
    payload: McaPayload,
    onSelectChunk: (ChunkLocation, File) -> Unit,
    onUpdateRequest: (GlobalAnvilUpdateRequest) -> Unit
) {

    val dimension = payload.dimension
    val type = payload.type

    val state by remember(payload) {
        mutableStateOf(
            when (payload.request) {
                is McaAnvilRequest -> selector.mcaState[payload]
                    ?: SelectorState.new(chunks.firstOrNull { it.toAnvilLocation() == payload.location })
                        .also { newState -> selector.mcaState[payload] = newState }
                is GlobalAnvilInitRequest -> selector.globalState[payload.dimension]
                    ?: SelectorState.new(payload.initial)
                        .also { newState -> selector.globalState[payload.dimension] = newState }
                is GlobalAnvilUpdateRequest -> selector.globalState[payload.dimension]
                    ?: SelectorState.new(initialChunk = null)
                        .also { newState -> selector.globalState[payload.dimension] = newState }
            }
        )
    }

    val validChunkX = chunks.map { chunk -> chunk.x }
    val validChunkZ = chunks.map { chunk -> chunk.z }

    val anvil by remember(state.selectedChunk, payload.location) {
        mutableStateOf(
            state.selectedChunk?.toAnvilLocation() ?: payload.location
        )
    }
    val anvils by remember(chunks) {
        mutableStateOf(
            chunks.map { it.toAnvilLocation() }.toSet().toList().sortedBy { "${it.x}.${it.z}" })
    }

    val surroundings by remember(anvil, anvils) {
        mutableStateOf(
            AnvilLocationSurroundings(
                base = anvil,
                left = AnvilLocation(anvil.x, anvil.z - 1).validate(anvils),
                right = AnvilLocation(anvil.x, anvil.z + 1).validate(anvils),
                above = AnvilLocation(anvil.x + 1, anvil.z).validate(anvils),
                below = AnvilLocation(anvil.x - 1, anvil.z).validate(anvils),
            )
        )
    }

    val isChunkValid = {
        val xInt = state.chunkXValue.text.toIntOrNull()
        val zInt = state.chunkZValue.text.toIntOrNull()
        Triple(xInt, zInt, validChunkX.contains(xInt) && validChunkZ.contains(zInt))
    }

    val isRegionExists: (AnvilLocation) -> Boolean = { location ->
        anvils.contains(location)
    }

    val isBlockValid = {
        val xInt = state.blockXValue.text.toIntOrNull()
        val zInt = state.blockZValue.text.toIntOrNull()
        Pair(xInt, zInt)
    }

    val resetChunk = {
        state.selectedChunk = null
        state.chunkXValue = TextFieldValue("-")
        state.chunkZValue = TextFieldValue("-")
        state.blockXValue = TextFieldValue("-")
        state.blockZValue = TextFieldValue("-")
    }

    val transformBlockCoordinate: (AnnotatedString) -> TransformedText = { annotated ->
        val int = annotated.text.toIntOrNull()

        val color =
            if (int == null) ThemedColor.Editor.Selector.Malformed
            else ThemedColor.Editor.Tag.General

        val spans = listOf(AnnotatedString.Range(SpanStyle(color = color), 0, annotated.text.length))

        TransformedText(AnnotatedString(annotated.text, spans), OffsetMapping.Identity)
    }

    val transformChunkCoordinate: (AnnotatedString, List<Int>) -> TransformedText = validate@ { annotated, criteria ->
        val int = annotated.text.toIntOrNull()

        val color =
            if (int == null) ThemedColor.Editor.Selector.Malformed
            else if (!criteria.contains(int)) ThemedColor.Editor.Selector.Invalid
            else ThemedColor.Editor.Tag.General

        val spans = listOf(AnnotatedString.Range(SpanStyle(color = color), 0, annotated.text.length))

        TransformedText(AnnotatedString(annotated.text, spans), OffsetMapping.Identity)
    }

    val invalidateBlockCoordinate: () -> Unit = {
        if (state.blockXValue.text != "-") state.blockXValue = TextFieldValue("-")
        if (state.blockZValue.text != "-") state.blockZValue = TextFieldValue("-")
    }

    val updateFromChunk: () -> Unit = update@ {
        val (x, z, isValid) = isChunkValid()
        val isInt = x != null && z != null
        val exists = if (isInt) chunks.contains(ChunkLocation(x!!, z!!)) else false

        val prevState = state.selectedChunk
        val newState =
            if (!isValid || !isInt || !exists) null
            else ChunkLocation(x!!, z!!)

        if (prevState == newState) return@update
        state.selectedChunk = newState
    }

    val updateFromBlock: () -> Unit = update@ {
        val (x, z) = isBlockValid()

        val isInt = x != null && z != null
        if (!isInt) return@update

        val chunk = BlockLocation(x!!, z!!).toChunkLocation()
        val (chunkX, chunkZ) = chunk.toStringPair()
        val chunkExists = chunks.contains(chunk)

        if (state.chunkXValue.text != chunkX) state.chunkXValue = TextFieldValue(chunkX)
        if (state.chunkZValue.text != chunkZ) state.chunkZValue = TextFieldValue(chunkZ)
        if (state.selectedChunk != chunk && chunkExists) state.selectedChunk = chunk
    }

    var openPressed by remember { mutableStateOf(false) }
    var openFocused by remember { mutableStateOf(false) }

    var regionPopupPos by remember { mutableStateOf(Offset.Zero) }
    var typePopupPos by remember { mutableStateOf(Offset.Zero) }
    var dimensionPopupPos by remember { mutableStateOf(Offset.Zero) }

    var popup by remember { mutableStateOf<String?>(null) }

    val maxYLimit = (if (dimension == WorldDimension.OVERWORLD) 319 else 124).toShort()
    val minYLimit = (if (dimension == WorldDimension.OVERWORLD) -64 else 0).toShort()

    val defaultYLimit = (if (dimension == WorldDimension.OVERWORLD) 319 else 89).toShort()

    val previewerYLimit = remember(dimension) { mutableStateOf(defaultYLimit) }

    Row(
        modifier = Modifier
            .background(ThemedColor.SelectorArea)
            .height(60.dp)
            .fillMaxWidth()
            .zIndex(10f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(10.dp))
        ChunkSelectorDropdown(
            "block:",
            disabled = popup != null
        ) { disabled ->
            CoordinateText("[")
            CoordinateInput(
                state.blockXValue,
                { state.blockXValue = it; updateFromBlock() },
                transformBlockCoordinate,
                disabled
            )
            CoordinateText(", ")
            CoordinateInput(
                state.blockZValue,
                { state.blockZValue = it; updateFromBlock() },
                transformBlockCoordinate,
                disabled
            )
            CoordinateText("]")
        }
        Spacer(modifier = Modifier.width(10.dp))
        ChunkSelectorDropdown(
            "chunk:",
            accent = true,
            valid = state.selectedChunk != null,
            disabled = popup != null
        ) { disabled ->
            CoordinateText("[")
            CoordinateInput(
                state.chunkXValue,
                { state.chunkXValue = it; invalidateBlockCoordinate(); updateFromChunk() },
                { transformChunkCoordinate(it, validChunkX) },
                disabled
            )
            CoordinateText(", ")
            CoordinateInput(
                state.chunkZValue,
                { state.chunkZValue = it; invalidateBlockCoordinate(); updateFromChunk() },
                { transformChunkCoordinate(it, validChunkZ) },
                disabled
            )
            CoordinateText("]")
        }
        Spacer(modifier = Modifier.width(10.dp))
        ChunkSelectorDropdown(
            "region:",
            onClick =
            if (anvils.size != 1) {
                regionDropdown@{
                    popup =
                        if (popup == "region") null
                        else "region"
                }
            } else null,
            modifier = Modifier.onGloballyPositioned {
                regionPopupPos = it.positionInParent()
            }
        ) {
            val (_, _, isValid) = isChunkValid()
            val regionExists = isRegionExists(state.selectedChunk?.toAnvilLocation() ?: payload.location)
            CoordinateText("r.")
            CoordinateText(
                "${state.selectedChunk?.toAnvilLocation()?.x ?: payload.location.x}",
                !isValid && !regionExists
            )
            CoordinateText(".")
            CoordinateText(
                "${state.selectedChunk?.toAnvilLocation()?.z ?: payload.location.z}",
                !isValid && !regionExists
            )
            CoordinateText(".mca")
        }
        if (payload.request !is McaAnvilRequest) {
            Spacer(modifier = Modifier.width(10.dp))
            ChunkSelectorDropdown(
                "type:",
                onClick = {
                    popup =
                        if (popup == "type") null
                        else "type"
                },
                modifier = Modifier.onGloballyPositioned {
                    typePopupPos = it.positionInParent()
                }
            ) {
                CoordinateText(type.name)
            }
            Spacer(modifier = Modifier.width(10.dp))
            ChunkSelectorDropdown(
                "dim:",
                onClick = {
                    popup =
                        if (popup == "dimension") null
                        else "dimension"
                },
                modifier = Modifier.onGloballyPositioned {
                    dimensionPopupPos = it.positionInParent()
                }
            ) {
                CoordinateText(dimension.name)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .background(
                    if (state.selectedChunk == null) Color.Transparent
                    else ThemedColor.clickable(openPressed, openFocused)
                )
                .onPointerEvent(PointerEventType.Enter) { openFocused = true }
                .onPointerEvent(PointerEventType.Exit) { openFocused = false }
                .onPointerEvent(PointerEventType.Press) { openPressed = true }
                .onPointerEvent(PointerEventType.Release) { openPressed = false }
                .height(60.dp)
                .width(60.dp)
                .alpha(if (state.selectedChunk == null) 0.5f else 1f)
                .mouseClickable {
                    val chunk = state.selectedChunk
                    val file =
                        if (chunk != null)
                            tree[dimension][type.pathName].find { it.name == "r.${chunk.toAnvilLocation().x}.${chunk.toAnvilLocation().z}.mca" }
                        else null
                    if (chunk != null && file != null) {
                        onSelectChunk(chunk, file)
                    }
                }
                .height(45.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                "->",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = ThemedColor.Editor.Selector.ButtonText,
                fontFamily = JetBrainsMono
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().zIndex(3f)
    ) {
        ChunkSelectorProperties(
            yRange = minYLimit..maxYLimit,
            yLimit = previewerYLimit,
            surroundings = surroundings,
            onMoveSurroundings = {
                resetChunk()
                onUpdateRequest(GlobalAnvilUpdateRequest(region = it))
            }
        ) { visibleState, loadState ->
            ChunksPreview(
                tree[dimension],
                previewerYLimit.value,
                state.selectedChunk,
                { chunks.contains(it) },
                rightClick = { visibleState.value = !visibleState.value },
                loadStateChanged = { loadState.value = it },
                forceAnvilLocation = if (payload.request is GlobalAnvilUpdateRequest) payload.location else null
            ) {
                state.chunkXValue = TextFieldValue(it.x.toString())
                state.chunkZValue = TextFieldValue(it.z.toString())
                state.blockXValue = TextFieldValue("-")
                state.blockZValue = TextFieldValue("-")
                state.selectedChunk = it
            }
        }

        PopupBackground(
            current = popup,
            onCloseRequest = { popup = null }
        )

        SelectorDropdown(
            ident = "region",
            items = anvils.toMutableList().apply {
                remove(state.selectedChunk?.toAnvilLocation() ?: payload.location)
            },
            indent = 3,
            current = popup,
            onCloseRequest = { popup = null },
            valueMapper = { "r.${it.x}.${it.z}.mca" },
            anchor = regionPopupPos,
        ) {
            resetChunk()
            onUpdateRequest(GlobalAnvilUpdateRequest(region = it))
        }

        SelectorDropdown(
            ident = "type",
            items = McaType.values().toMutableList().apply { remove(type) },
            indent = 1,
            current = popup,
            onCloseRequest = { popup = null },
            valueMapper = { it.name },
            anchor = typePopupPos,
        ) {
            resetChunk()
            onUpdateRequest(GlobalAnvilUpdateRequest(type = it))
        }

        SelectorDropdown(
            ident = "dimension",
            items = WorldDimension.values().toMutableList().apply { remove(dimension) },
            indent = 0,
            current = popup,
            onCloseRequest = { popup = null },
            valueMapper = { it.name },
            anchor = dimensionPopupPos,
        ) {
            resetChunk()
            onUpdateRequest(GlobalAnvilUpdateRequest(dimension = it))
        }
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BoxScope.ChunkSelectorProperties(
    yRange: IntRange,
    yLimit: MutableState<Short>,
    surroundings: AnvilLocationSurroundings,
    onMoveSurroundings: (AnvilLocation) -> Unit,
    content: @Composable BoxScope.(MutableState<Boolean>, MutableState<Boolean>) -> Unit
) {

    val alignment = Alignment.TopStart

    val visibleState = remember { mutableStateOf(false) }
    val loadState = remember { mutableStateOf(false) }

    Box(modifier = Modifier.wrapContentSize().requiredSizeIn(minWidth = 600.dp, minHeight = 600.dp).align(Alignment.Center)) {
        content(visibleState, loadState)

        AnimatedVisibility(
            visible = visibleState.value,
            enter = fadeIn(tween(150, easing = LinearEasing)),
            exit = fadeOut(tween(150, easing = LinearEasing)),
            modifier = Modifier.align(alignment).fillMaxHeight().aspectRatio(1f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.align(alignment).requiredSize(600.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().scale(scaleX = 1f, scaleY = 0.5f)
                            .absoluteOffset(y = (-300).dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color.Black, Color.Transparent),
                                    Offset.Zero,
                                    radius = 600f
                                )
                            )
                            .align(alignment)
                    )
                    Column(
                        modifier = Modifier
                            .width(350.dp)
                            .align(alignment)
                            .padding(top = 10.dp, start = 20.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            ChunkInvestButton("above", surroundings.above, onMoveSurroundings)
                            ChunkInvestButton("left", surroundings.left, onMoveSurroundings)
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            ChunkInvestButton("below", surroundings.below, onMoveSurroundings)
                            ChunkInvestButton("right", surroundings.right, onMoveSurroundings)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("yLimit = ", color = ThemedColor.ChunkSelectorPropertyKey, fontFamily = JetBrainsMono)
                            Box(
                                modifier = Modifier
                                    .onPointerEvent(PointerEventType.Scroll) {
                                        yLimit.value = (yLimit.value + this.currentEvent.changes[0].scrollDelta.y.toInt()).coerceIn(yRange).toShort()
                                    }
                                    .padding(top = 3.dp, bottom = 3.dp)
                            ) {
                                val text = "${yLimit.value}".padStart(3, '_')
                                Text(
                                    AnnotatedString(
                                        text,
                                        listOf(
                                            AnnotatedString.Range(
                                                SpanStyle(
                                                    color = ThemedColor.from(ThemedColor.Editor.Tag.General, alpha = 75)
                                                ),
                                                start = 0,
                                                end = 3 - "${yLimit.value}".length
                                            )
                                        )
                                    ),
                                    color = ThemedColor.Editor.Tag.General,
                                    fontFamily = JetBrainsMono
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowScope.ChunkInvestButton(direction: String, dest: AnvilLocation? = null, move: (AnvilLocation) -> Unit) {
    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
        Text("$direction = ", color = ThemedColor.ChunkSelectorPropertyKey, fontFamily = JetBrainsMono, fontSize = 18.sp)
        if (dest == null) {
            Text("null", color = ThemedColor.Editor.Tag.List, fontFamily = JetBrainsMono, fontSize = 18.sp)
        } else {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .background(Color(88, 88, 88, 125), shape = RoundedCornerShape(2.dp))
                    .padding(start = 5.dp, end = 5.dp)
                    .mouseClickable { move(dest) }
            ) {
                Text("Go", color = ThemedColor.Editor.Tag.General, fontFamily = JetBrainsMono, fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.width(20.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PopupBackground(
    current: String?,
    onCloseRequest: MouseClickScope.() -> Unit,
) {
    var state by remember { mutableStateOf(-1) }
    val alpha by animateFloatAsState(if (state == 1) 1f else 0f, tween(75, easing = LinearEasing))

    LaunchedEffect(current) {
        if (current != null) state = 1
        else if (state != -1) {
            state = 0
            delay(125)
            state = -1
        }
    }

    if (state == -1) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemedColor.from(Color.Black, alpha = (alpha * 150).toInt()))
            .mouseClickable(onClick = onCloseRequest)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T>SelectorDropdown(
    ident: String,
    items: List<T>,
    indent: Int,
    current: String?,
    onCloseRequest: MouseClickScope.() -> Unit,
    valueMapper: (T) -> String,
    anchor: Offset,
    onClick: (T) -> Unit
) {
    var state by remember { mutableStateOf(-1) }
    val alpha by animateFloatAsState(if (state == 1) 1f else 0f, tween(75, easing = LinearEasing))

    LaunchedEffect(ident, current) {
        if (ident == current) state = 1
        else if (state != -1) {
            state = 0
            delay(125)
            state = -1
        }
    }

    var width by remember { mutableStateOf<Int?>(null) }

    val s = rememberScrollState()

    if (state == -1) return

    Box (modifier = Modifier.fillMaxSize().alpha(alpha).padding(top = 7.dp)) {
        Column(
            modifier = Modifier
                .absoluteOffset(x = anchor.x.dp)
                .padding(start = (8.7f * (indent + 4)).dp)
                .verticalScroll(s)
                .onGloballyPositioned { width = it.size.width }
        ) {
            for (item in items) {
                ChunkSelectorDropdown(
                    "",
                    onClick = { onClick(item); onCloseRequest() },
                    modifier = Modifier.shadow(7.dp)
                        .let { modifier -> width.let { if (it == null) modifier else modifier.width(it.dp) } }
                ) {
                    CoordinateText(valueMapper(item))
                    Spacer(modifier = Modifier.width(5.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun RowScope.CoordinateText(text: String, invalid: Boolean = false) {
    Text(
        text,
        fontSize = 16.sp,
        color = if (invalid) ThemedColor.Editor.Selector.Invalid else ThemedColor.Editor.Tag.General,
        fontFamily = JetBrainsMono,
        modifier = Modifier.focusable(false)
    )
}

@Composable
fun RowScope.CoordinateInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    transformer: (AnnotatedString) -> TransformedText = { TransformedText(it, OffsetMapping.Identity) },
    disabled: Boolean = false
) {
    BasicTextField(
        value,
        onValueChange,
        textStyle = TextStyle(
            fontSize = 16.sp,
            color = ThemedColor.Editor.Tag.General,
            fontFamily = JetBrainsMono
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        cursorBrush = SolidColor(ThemedColor.Editor.Tag.General),
        visualTransformation = transformer,
        enabled = !disabled,
        modifier = Modifier
            .width((value.text.length.coerceAtLeast(1) * 9.75).dp)
            .focusable(false)
            .onFocusChanged {
                if (!it.isFocused && value.text.isEmpty()) onValueChange(TextFieldValue("-"))
            }
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun ChunkSelectorDropdown(
    prefix: String,
    accent: Boolean = false,
    valid: Boolean = true,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
    onClick: (MouseClickScope.() -> Unit)? = null,
    content: @Composable RowScope.(Boolean) -> Unit
) {
    var hover by remember { mutableStateOf(false) }

    Row (
        modifier = Modifier.then(modifier)
            .background(
                ThemedColor.from(
                    ThemedColor.Editor.Selector.background(accent, valid, onClick != null && hover),
                    alpha = if (disabled) (255 * 0.6f).toInt() else 255
                ),
                RoundedCornerShape(4.dp)
            )
            .height(40.dp)
            .alpha(if (disabled) 0.6f else 1f)
            .let {
                if (onClick != null)
                    it.mouseClickable(onClick = onClick)
                        .onPointerEvent(PointerEventType.Enter) { hover = true }
                        .onPointerEvent(PointerEventType.Exit) { hover = false }
                else it
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            prefix,
            fontSize = 14.sp,
            color = ThemedColor.Editor.Selector.Normal,
            fontFamily = JetBrainsMono
        )
        Spacer(modifier = Modifier.width(8.dp))
        content(disabled)
        Spacer(modifier = Modifier.width(12.dp))
    }
}
