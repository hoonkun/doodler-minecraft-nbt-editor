package composables.stateful.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import composables.stateless.editor.*
import composables.states.editor.world.*
import composables.states.editor.world.extensions.toRanges
import composables.themed.*
import doodler.anvil.AnvilLocation
import doodler.anvil.AnvilWorker
import doodler.anvil.BlockLocation
import doodler.anvil.ChunkLocation
import doodler.file.WorldTree
import doodler.file.IOUtils
import doodler.file.WorldDimension
import doodler.file.WorldDimensionTree
import keys
import kotlinx.coroutines.launch
import doodler.nbt.TagType
import doodler.nbt.tag.CompoundTag
import doodler.nbt.tag.DoubleTag
import doodler.nbt.tag.ListTag
import doodler.nbt.tag.StringTag
import java.io.File

@Composable
fun WorldEditor(
    worldPath: String
) {
    val states = rememberWorldEditorState()

    if (states.worldSpec.tree == null)
        states.worldSpec.tree = IOUtils.load(worldPath)

    val levelInfo = IOUtils.readLevel(states.worldSpec.requireTree.level.readBytes())["Data"]?.getAs<CompoundTag>()

    if (states.worldSpec.name == null)
        states.worldSpec.name = levelInfo!!["LevelName"]
            ?.getAs<StringTag>()
            ?.value

    if (states.worldSpec.tree == null || states.worldSpec.name == null) {
        // TODO: Handle Loading or Parse Error here
        return
    }

    val tree = states.worldSpec.requireTree
    val name = states.worldSpec.requireName

    val onOpenRequest: (OpenRequest) -> Unit = handleRequest@ { request ->
        when (request) {
            is NbtOpenRequest -> {
                if (states.editor.hasItem(request.target.ident)) {
                    states.editor.select(request.target)
                } else {
                    states.editor.open(request.target)
                }
            }
            is AnvilOpenRequest -> {
                if (!states.editor.hasItem("ANVIL_SELECTOR")) {
                    states.editor.open(SelectorItem().apply { from = request })
                } else {
                    val selector = states.editor["ANVIL_SELECTOR"] ?: return@handleRequest
                    if (selector !is SelectorItem) return@handleRequest

                    if (request is GlobalAnvilInitRequest && selector.baseGlobalMcaInfo != null)
                        selector.from = GlobalAnvilUpdateRequest()
                    else
                        selector.from = request

                    states.editor.select(selector)
                }
            }
        }
    }

    MaterialTheme {
        MainColumn {
            TopBar { TopBarText(name) }

            MainArea {
                MainFiles {
                    WorldTreeView(name, tree, onOpenRequest)
                }
                MainContents {
                    if (states.editor.items.size == 0) {
                        NoFileSelected(name)
                    } else {
                        Editor(levelInfo, tree, states.editor)
                    }
                }
            }

            BottomBar {
                Spacer(modifier = Modifier.weight(1f))
                BottomBarText("by kiwicraft")
            }
        }
    }
}

@Composable
fun BoxScope.Editor(
    levelInfo: CompoundTag?,
    tree: WorldTree,
    editor: Editor,
) {
    EditorRoot {
        SpeciesTabGroup(
            editor.items.map { TabData(editor.selected == it, it) },
            { editor.select(it) },
            { editor.close(it) }
        )
        Editables {
            val selected = editor.selected
            if (selected is NbtItem) EditableField(selected)
            else if (selected is SelectorItem) {
                McaMap(
                    levelInfo, selected, tree,
                    open@ { location, file ->
                        if (editor.hasItem("${file.absolutePath}/c.${location.x}.${location.z}")) return@open

                        val root = AnvilWorker.loadChunk(location, file.readBytes()) ?: return@open
                        editor.open(AnvilNbtItem(NbtState.new(root), file, location))
                    },
                    update@ {
                        val selector = editor["ANVIL_SELECTOR"] ?: return@update
                        if (selector !is SelectorItem) return@update

                        selector.from = it
                    }
                )
            }
        }
    }
}

@Composable
fun BoxScope.McaMap(
    levelInfo: CompoundTag?,
    selector: SelectorItem,
    tree: WorldTree,
    onOpenRequest: (ChunkLocation, File) -> Unit,
    onUpdateRequest: (GlobalAnvilUpdateRequest) -> Unit
) {
    val request = selector.from

    val data by remember(request) { mutableStateOf(
        when (request) {
            is McaAnvilRequest -> {
                val file = request.file
                val location = request.location

                val dimension = WorldDimension[file.parentFile.parentFile.name]

                val type = WorldDimensionTree.McaType[file.parentFile.name]

                val chunks = AnvilWorker.loadChunkList(location, file.readBytes())

                Pair(chunks, McaInfo(request, dimension, type, location, file))
            }
            is GlobalAnvilInitRequest -> {
                val player = levelInfo?.get("Player")?.getAs<CompoundTag>()
                val dimensionId = player?.get("Dimension")?.getAs<StringTag>()?.value

                val pos = player?.get("Pos")?.getAs<ListTag>()
                val x = pos?.get(0)?.getAs<DoubleTag>()?.value?.toInt()
                val z = pos?.get(2)?.getAs<DoubleTag>()?.value?.toInt()

                if (player == null || dimensionId == null || x == null || z == null)
                    throw DoodleException("Internal Error", null, "Could not find dimension data of Player.")

                val dimension = WorldDimension.namespace(dimensionId)
                val type = WorldDimensionTree.McaType.TERRAIN
                val initial = BlockLocation(x, z)
                val location = initial.toChunkLocation().toAnvilLocation()
                val file = tree[dimension][WorldDimensionTree.McaType.TERRAIN.pathName]
                    .find { it.name == "r.${location.x}.${location.z}.mca" }
                    ?: throw DoodleException("Internal Error", null, "Could not find terrain region file which player exists.")

                val chunks = tree[dimension][WorldDimensionTree.McaType.TERRAIN.pathName].map {
                    val segments = it.name.split(".")
                    val itLocation = AnvilLocation(segments[1].toInt(), segments[2].toInt())
                    AnvilWorker.loadChunkList(itLocation, it.readBytes())
                }.toList().flatten()

                val mcaInfo = McaInfo(request, dimension, type, location, file, initial)
                selector.baseGlobalMcaInfo = mcaInfo

                Pair(chunks, mcaInfo)
            }
            is GlobalAnvilUpdateRequest -> {
                val base = selector.baseGlobalMcaInfo ?: throw DoodleException("Internal Error", null, "Failed to update null McaInfo")

                val newMcaInfo = McaInfo.from(
                    base,
                    request = request,
                    dimension = request.dimension,
                    type = request.type,
                    location = request.region,
                    file = tree[request.dimension ?: base.dimension][(request.type ?: base.type).pathName].find {
                        val reg = request.region
                        if (reg != null) it.name == "r.${reg.x}.${reg.z}.mca" else false
                    },
                    initial = null
                )

                selector.baseGlobalMcaInfo = newMcaInfo

                val chunks = tree[request.dimension ?: newMcaInfo.dimension][(request.type ?: newMcaInfo.type).pathName].map {
                    val segments = it.name.split(".")
                    val itLocation = AnvilLocation(segments[1].toInt(), segments[2].toInt())
                    AnvilWorker.loadChunkList(itLocation, it.readBytes())
                }.toList().flatten()

                Pair(chunks, newMcaInfo)
            }
            else -> {
                throw DoodleException("Internal Error", null, "Cannot initialize McaInfo.")
            }
        }
    ) }

    val (chunks, mcaInfo) = data

    Column(modifier = Modifier.fillMaxSize()) {
        ChunkSelector(
            chunks, tree, selector,
            mcaInfo,
            onOpenRequest,
            onUpdateRequest
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.ChunkSelector(
    chunks: List<ChunkLocation>,
    tree: WorldTree,
    selector: SelectorItem,
    mcaInfo: McaInfo,
    onSelectChunk: (ChunkLocation, File) -> Unit,
    onUpdateRequest: (GlobalAnvilUpdateRequest) -> Unit
) {

    val dimension = mcaInfo.dimension
    val type = mcaInfo.type

    val state by remember(mcaInfo) { mutableStateOf(
        when (mcaInfo.request) {
            is McaAnvilRequest -> selector.mcaState[mcaInfo]
                ?: SelectorState.new(chunks.firstOrNull { it.toAnvilLocation() == mcaInfo.location }).also { newState -> selector.mcaState[mcaInfo] = newState }
            is GlobalAnvilInitRequest -> selector.globalState[mcaInfo.dimension]
                ?: SelectorState.new(mcaInfo.initial).also { newState -> selector.globalState[mcaInfo.dimension] = newState }
            is GlobalAnvilUpdateRequest -> selector.globalState[mcaInfo.dimension]
                ?: SelectorState.new(initialChunk = null).also { newState -> selector.globalState[mcaInfo.dimension] = newState }
        }
    ) }

    val validChunkX = chunks.map { chunk -> chunk.x }
    val validChunkZ = chunks.map { chunk -> chunk.z }

    val regions by remember(chunks) { mutableStateOf(chunks.map { it.toAnvilLocation() }.toSet().toList().sortedBy { "${it.x}.${it.z}" }) }

    val isChunkValid = {
        val xInt = state.chunkXValue.text.toIntOrNull()
        val zInt = state.chunkZValue.text.toIntOrNull()
        Triple(xInt, zInt, validChunkX.contains(xInt) && validChunkZ.contains(zInt))
    }

    val isRegionExists: (AnvilLocation) -> Boolean = { location ->
        regions.contains(location)
    }

    val isBlockValid = {
        val xInt = state.blockXValue.text.toIntOrNull()
        val zInt = state.blockZValue.text.toIntOrNull()
        Pair(xInt, zInt)
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

        if (state.chunkXValue.text != chunkX) state.chunkXValue = TextFieldValue(chunkX)
        if (state.chunkZValue.text != chunkZ) state.chunkZValue = TextFieldValue(chunkZ)
        if (state.selectedChunk != chunk) state.selectedChunk = chunk
    }

    var openPressed by remember { mutableStateOf(false) }
    var openFocused by remember { mutableStateOf(false) }

    var regionPopupPos by remember { mutableStateOf(Offset.Zero) }
    var typePopupPos by remember { mutableStateOf(Offset.Zero) }
    var dimensionPopupPos by remember { mutableStateOf(Offset.Zero) }

    var popup by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = Modifier
            .background(ThemedColor.SelectorArea)
            .height(60.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(10.dp))
        ChunkSelectorDropdown("block:") {
            CoordinateText("[")
            CoordinateInput(
                state.blockXValue,
                { state.blockXValue = it; updateFromBlock() },
                transformBlockCoordinate
            )
            CoordinateText(", ")
            CoordinateInput(
                state.blockZValue,
                { state.blockZValue = it; updateFromBlock() },
                transformBlockCoordinate
            )
            CoordinateText("]")
        }
        Spacer(modifier = Modifier.width(10.dp))
        ChunkSelectorDropdown("chunk:", true, state.selectedChunk != null) {
            CoordinateText("[")
            CoordinateInput(
                state.chunkXValue,
                { state.chunkXValue = it; invalidateBlockCoordinate(); updateFromChunk() },
                { transformChunkCoordinate(it, validChunkX) }
            )
            CoordinateText(", ")
            CoordinateInput(
                state.chunkZValue,
                { state.chunkZValue = it; invalidateBlockCoordinate(); updateFromChunk() },
                { transformChunkCoordinate(it, validChunkZ) }
            )
            CoordinateText("]")
        }
        Spacer(modifier = Modifier.width(10.dp))
        ChunkSelectorDropdown(
            "region:",
            onClick = {
                popup =
                    if (popup == "region") null
                    else "region"
            },
            modifier = Modifier.onGloballyPositioned {
                regionPopupPos = it.positionInParent()
            }
        ) {
            val (_, _, isValid) = isChunkValid()
            val regionExists = isRegionExists(state.selectedChunk?.toAnvilLocation() ?: mcaInfo.location)
            CoordinateText("r.")
            CoordinateText("${state.selectedChunk?.toAnvilLocation()?.x ?: mcaInfo.location.x}", !isValid && !regionExists)
            CoordinateText(".")
            CoordinateText("${state.selectedChunk?.toAnvilLocation()?.z ?: mcaInfo.location.z}", !isValid && !regionExists)
            CoordinateText(".mca")
        }
        if (mcaInfo.request !is McaAnvilRequest) {
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

    Box(modifier = Modifier.fillMaxSize()) {
        RegionPreview(
            tree[dimension],
            dimension,
            state.selectedChunk,
            { chunks.contains(it) },
            forceAnvilLocation = if (mcaInfo.request is GlobalAnvilUpdateRequest) mcaInfo.location else null
        ) {
            state.chunkXValue = TextFieldValue(it.x.toString())
            state.chunkZValue = TextFieldValue(it.z.toString())
            state.blockXValue = TextFieldValue("-")
            state.blockZValue = TextFieldValue("-")
            state.selectedChunk = it
        }

        val resetChunk = {
            state.selectedChunk = null
            state.chunkXValue = TextFieldValue("-")
            state.chunkZValue = TextFieldValue("-")
            state.blockXValue = TextFieldValue("-")
            state.blockZValue = TextFieldValue("-")
        }

        if (popup == "region") {
            SelectorDropdown(
                items = regions.toMutableList().apply {
                    remove(state.selectedChunk?.toAnvilLocation() ?: mcaInfo.location)
                },
                indent = 3,
                onCloseRequest = { popup = null },
                valueMapper = { "r.${it.x}.${it.z}.mca" },
                anchor = regionPopupPos,
            ) {
                resetChunk()
                onUpdateRequest(GlobalAnvilUpdateRequest(region = it))
            }
        }
        if (popup == "type") {
            SelectorDropdown(
                items = WorldDimensionTree.McaType.values().toMutableList().apply { remove(type) },
                indent = 1,
                onCloseRequest = { popup = null },
                valueMapper = { it.name },
                anchor = typePopupPos,
            ) {
                resetChunk()
                onUpdateRequest(GlobalAnvilUpdateRequest(type = it))
            }
        }
        if (popup == "dimension") {
            SelectorDropdown(
                items = WorldDimension.values().toMutableList().apply { remove(dimension) },
                indent = 0,
                onCloseRequest = { popup = null },
                valueMapper = { it.name },
                anchor = dimensionPopupPos,
            ) {
                resetChunk()
                onUpdateRequest(GlobalAnvilUpdateRequest(dimension = it))
            }
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T>SelectorDropdown(
    items: List<T>,
    indent: Int,
    onCloseRequest: MouseClickScope.() -> Unit,
    valueMapper: (T) -> String,
    anchor: Offset,
    onClick: (T) -> Unit
) {
    var width by remember { mutableStateOf<Int?>(null) }

    val s = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemedColor.from(Color.Black, alpha = 150))
            .padding(top = 7.dp)
            .mouseClickable(onClick = onCloseRequest)
    ) {
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BoxScope.EditableField(
    species: NbtItem,
) {
    val coroutineScope = rememberCoroutineScope()

    val state = species.state

    val doodles = state.doodles.create()
    val creation = doodles.find { it is VirtualDoodle } as VirtualDoodle?
    val uiState = state.ui
    val lazyColumnState = state.lazyState

    val onToggle: (ActualDoodle) -> Unit = click@ { doodle ->
        if (doodle !is NbtDoodle) return@click
        if (!doodle.tag.canHaveChildren) return@click

        if (!doodle.expanded) doodle.expand()
        else doodle.collapse(state.ui.selected)
    }

    val onSelect: (ActualDoodle) -> Unit = { doodle ->
        if (!uiState.selected.contains(doodle)) {
            if (keys.contains(androidx.compose.ui.input.key.Key.CtrlLeft)) uiState.addToSelected(doodle)
            else if (keys.contains(androidx.compose.ui.input.key.Key.ShiftLeft)) {
                val lastSelected = uiState.getLastSelected()
                if (lastSelected == null) uiState.addToSelected(doodle)
                else {
                    val from = doodles.indexOf(lastSelected)
                    val to = doodles.indexOf(doodle)
                    uiState.addRangeToSelected(doodles.filterIsInstance<ActualDoodle>().slice(
                        if (from < to) from + 1 until to + 1
                        else to until from
                    ))
                }
            } else uiState.setSelected(doodle)
        } else {
            if (keys.contains(androidx.compose.ui.input.key.Key.CtrlLeft) || uiState.selected.size == 1)
                uiState.removeFromSelected(doodle)
            else if (uiState.selected.size > 1)
                uiState.setSelected(doodle)
        }
    }

    val treeCollapse: (NbtDoodle) -> Unit = { target ->
        target.collapse(state.ui.selected)
        val baseIndex = doodles.indexOf(target)
        if (lazyColumnState.firstVisibleItemIndex > baseIndex) {
            coroutineScope.launch { lazyColumnState.scrollToItem(baseIndex) }
        }
    }

    val treeViewTarget = if (uiState.focusedTree == null) uiState.focusedTreeView else uiState.focusedTree
    if (treeViewTarget != null && lazyColumnState.firstVisibleItemIndex > doodles.indexOf(treeViewTarget)) {
        DepthPreviewNbtItem(treeViewTarget, uiState) {
            val index = doodles.indexOf(treeViewTarget)
            coroutineScope.launch {
                lazyColumnState.scrollToItem(index)
            }
            uiState.unFocusTreeView(treeViewTarget)
            uiState.unFocusTree(treeViewTarget)
            uiState.focusDirectly(treeViewTarget)
        }
    }

    val onToolBarMove: AwaitPointerEventScope.(PointerEvent) -> Unit = {
        val target = uiState.focusedDirectly
        if (target != null) uiState.unFocusDirectly(target)
    }

    LazyColumn (state = lazyColumnState) {
        items(doodles, key = { item -> item.path }) { item ->
            if (item is ActualDoodle)
                ActualNbtItem(
                    item,
                    uiState,
                    onToggle, onSelect, treeCollapse,
                    creation != null,
                    creation != null && item != creation.parent
                )
            else if (item is VirtualDoodle)
                VirtualNbtItem(item, state)
        }
    }

    SelectedInWholeFileIndicator(doodles.filterIsInstance<ActualDoodle>(), uiState.selected) {
        coroutineScope.launch { lazyColumnState.scrollToItem(doodles.indexOf(it)) }
    }

    Column(
        modifier = Modifier.align(Alignment.BottomStart)
    ) {
        if (state.currentLogState.value != null) {
            Log(state.currentLogState)
        }
    }

    if (creation != null) return

    Row(
        modifier = Modifier.align(Alignment.TopEnd).padding(30.dp)
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize()
        ) {
            UndoRedoActionColumn(state, onToolBarMove)
            Spacer(modifier = Modifier.height(20.dp))
            IndexChangeActionColumn(state, onToolBarMove)
        }
        Column(
            modifier = Modifier
                .wrapContentSize()
                .padding(start = 20.dp)
        ) {
            NormalActionColumn(state, onToolBarMove)
            Spacer(modifier = Modifier.height(20.dp))
            CreateActionColumn(state, uiState.selected.firstOrNull() as? NbtDoodle, onToolBarMove)
        }
    }

}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.UndoRedoActionColumn(
    state: NbtState,
    onToolBarMove: AwaitPointerEventScope.(PointerEvent) -> Unit
) {
    val actions = state.actions

    Column(
        modifier = Modifier
            .background(ThemedColor.Editor.Action.Background, RoundedCornerShape(4.dp))
            .wrapContentSize()
            .onPointerEvent(PointerEventType.Move, onEvent = onToolBarMove)
            .padding(5.dp)
    ) {
        NbtActionButton(disabled = !actions.history.canBeUndo, onClick = { actions.withLog { history.undo() } }) {
            NbtText("UND", ThemedColor.Editor.Tag.General)
        }
        NbtActionButton(disabled = !actions.history.canBeRedo, onClick = { actions.withLog { history.redo() } }) {
            NbtText("RED", ThemedColor.Editor.Tag.General)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.IndexChangeActionColumn(
    state: NbtState,
    onToolBarMove: AwaitPointerEventScope.(PointerEvent) -> Unit
) {
    val available =
        state.ui.selected.map { it.index }.toRanges().size == 1 &&
        state.ui.selected.map { it.parent }.toSet().size == 1

    val canMoveUp = (state.ui.selected.firstOrNull()?.index ?: 0) != 0
    val canMoveDown = (state.ui.selected.lastOrNull()?.let { it.index == it.parent?.expandedItems?.size?.minus(1) }) != true

    Column(
        modifier = Modifier
            .background(ThemedColor.Editor.Action.Background, RoundedCornerShape(4.dp))
            .wrapContentSize()
            .onPointerEvent(PointerEventType.Move, onEvent = onToolBarMove)
            .padding(5.dp)
    ) {
        NbtActionButton(
            disabled = !(available && canMoveUp),
            onClick = { state.actions.elevator.moveUp(state.ui.selected) }
        ) {
            NbtText("<- ", ThemedColor.Editor.Tag.General, rotate = 90f, multiplier = 1)
        }
        NbtActionButton(
            disabled = !(available && canMoveDown),
            onClick = { state.actions.elevator.moveDown(state.ui.selected) }
        ) {
            NbtText(" ->", ThemedColor.Editor.Tag.General, rotate = 90f, multiplier = -1)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ColumnScope.CreateActionColumn(
    state: NbtState,
    selected: NbtDoodle?,
    onToolBarMove: AwaitPointerEventScope.(PointerEvent) -> Unit
) {
    val actions = state.actions
    val tag = selected?.tag ?: state.rootDoodle.tag

    Column(
        modifier = Modifier
            .background(ThemedColor.Editor.Action.Background, RoundedCornerShape(4.dp))
            .wrapContentSize()
            .onPointerEvent(PointerEventType.Move, onEvent = onToolBarMove)
            .padding(5.dp)
    ) {
        TagCreationButton(tag, TagType.TAG_BYTE, actions)
        TagCreationButton(tag, TagType.TAG_SHORT, actions)
        TagCreationButton(tag, TagType.TAG_INT, actions)
        TagCreationButton(tag, TagType.TAG_LONG, actions)
        TagCreationButton(tag, TagType.TAG_FLOAT, actions)
        TagCreationButton(tag, TagType.TAG_DOUBLE, actions)
        TagCreationButton(tag, TagType.TAG_BYTE_ARRAY, actions)
        TagCreationButton(tag, TagType.TAG_INT_ARRAY, actions)
        TagCreationButton(tag, TagType.TAG_LONG_ARRAY, actions)
        TagCreationButton(tag, TagType.TAG_STRING, actions)
        TagCreationButton(tag, TagType.TAG_LIST, actions)
        TagCreationButton(tag, TagType.TAG_COMPOUND, actions)
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.NormalActionColumn(
    state: NbtState,
    onToolBarMove: AwaitPointerEventScope.(PointerEvent) -> Unit
) {
    val actions = state.actions

    val available = state.ui.selected.isNotEmpty()

    Column(
        modifier = Modifier
            .background(ThemedColor.Editor.Action.Background, RoundedCornerShape(4.dp))
            .wrapContentSize()
            .onPointerEvent(PointerEventType.Move, onEvent = onToolBarMove)
            .padding(5.dp)
    ) actionColumn@{
        NbtActionButton(
            disabled = !available,
            onClick = { state.actions.withLog { deleter.delete() } }
        ) {
            NbtText("DEL", ThemedColor.Editor.Action.Delete)
        }
        NbtActionButton(
            disabled = !available || actions.clipboard.pasteTarget == CannotBePasted,
            onClick = { actions.withLog { clipboard.yank() } }
        ) {
            NbtText("CPY", ThemedColor.Editor.Tag.General)
        }
        NbtActionButton(
            disabled = !available || (actions.clipboard.stack.size == 0 || !actions.clipboard.pasteEnabled()),
            onClick = { actions.withLog { clipboard.paste() } }
        ) {
            NbtText("PST", ThemedColor.Editor.Tag.General)
        }
        NbtActionButton(
            disabled = !available || (state.ui.selected.firstOrNull() as? NbtDoodle?)?.let { it.tag.name != null || it.tag.canHaveChildren } != true,
            onClick = { actions.withLog { editor.prepare() } }
        ) {
            NbtText("EDT", ThemedColor.Editor.Tag.General)
        }
    }
}

@Composable
private fun BoxScope.SelectedInWholeFileIndicator(doodles: List<ActualDoodle>, selected: List<ActualDoodle>, scrollTo: (ActualDoodle) -> Unit) {
    val fraction = 1f / (doodles.size - 1)

    Box (
        modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight().wrapContentWidth()
    ) {
        for (item in selected) {
            val top = doodles.indexOf(item) * fraction
            SelectedEach(item, top, fraction, scrollTo)
        }
    }

    Box (
        modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight().wrapContentWidth().alpha(0.5f)
    ) {
        for (item in selected) {
            val top = doodles.indexOf(item) * fraction
            Column (modifier = Modifier.zIndex(1000f).width(20.dp).align(Alignment.TopEnd)) {
                if (top > 0) Spacer(modifier = Modifier.weight(top))
                Box(
                    modifier = Modifier
                        .fillMaxHeight(fraction)
                        .defaultMinSize(3.dp).fillMaxWidth()
                        .background(ThemedColor.Editor.ScrollIndicatorSelected)
                ) {}
                if (top < 1) Spacer(modifier = Modifier.weight(1 - top))
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun BoxScope.SelectedEach(
    item: ActualDoodle,
    top: Float,
    fraction: Float,
    scrollTo: (ActualDoodle) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    var previewFocused by remember { mutableStateOf(false) }
    var previewPressed by remember { mutableStateOf(false) }

    Column (modifier = Modifier.zIndex(1000f).width(20.dp).align(Alignment.TopEnd).alpha(0.5f)) {
        if (top > 0) Spacer(modifier = Modifier.weight(top))
        Box(
            modifier = Modifier
                .fillMaxHeight(fraction)
                .defaultMinSize(3.dp).fillMaxWidth()
                .onPointerEvent(PointerEventType.Enter) { focused = true }
                .onPointerEvent(PointerEventType.Exit) { focused = false }
                .mouseClickable(onClick = { scrollTo(item) })
        ) {}
        if (top < 1) Spacer(modifier = Modifier.weight(1 - top))
    }

    if (!(focused || previewFocused)) return

    Column(modifier = Modifier.zIndex(999f).wrapContentSize().align(Alignment.TopEnd).padding(end = 20.dp)) {
        if (top > 0) Spacer(modifier = Modifier.weight(top))
        Box (modifier = Modifier
            .background(ThemedColor.EditorArea)
            .wrapContentSize()
            .zIndex(999f)
        ) {
            Box(modifier = Modifier
                .border(2.dp, ThemedColor.Editor.TreeBorder)
                .onPointerEvent(PointerEventType.Enter) { previewFocused = true }
                .onPointerEvent(PointerEventType.Exit) { previewFocused = false }
                .onPointerEvent(PointerEventType.Press) { previewPressed = true }
                .onPointerEvent(PointerEventType.Release) { previewPressed = false }
                .mouseClickable(onClick = { scrollTo(item); previewFocused = false })
                .background(ThemedColor.Editor.normalItem(previewPressed, previewFocused))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(start = 20.dp, end = 20.dp)
                        .height(50.dp)
                ) {
                    ActualNbtItemContent(item, false)
                }
            }
        }
        if (top < 1) Spacer(modifier = Modifier.weight(1 - top))
    }
}
