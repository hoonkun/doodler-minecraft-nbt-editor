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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
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
    val scrollState = rememberScrollState()

    val states = rememberWorldEditorState()

    if (states.worldSpec.tree == null)
        states.worldSpec.tree = IOUtils.load(worldPath)

    val levelInfo = IOUtils.readLevel(states.worldSpec.requireTree.level.readBytes())["Data"]

    if (states.worldSpec.name == null)
        states.worldSpec.name = levelInfo
            ?.getAs<CompoundTag>()!!["LevelName"]
            ?.getAs<StringTag>()
            ?.value

    if (states.worldSpec.tree == null || states.worldSpec.name == null) {
        // TODO: Handle Loading or Parse Error here
        return
    }

    val tree = states.worldSpec.requireTree
    val name = states.worldSpec.requireName

    val onCategoryItemClick: (PhylumCategoryItemData) -> Unit = lambda@ { data ->
        states.phylum.list.find { it.ident == data.key }?.let {
            states.phylum.species = it
            return@lambda
        }

        val newHolder =
            if (data.holderType == SpeciesHolder.Type.Single) {
                SingleSpeciesHolder(
                    data.key, data.format, data.contentType,
                    NbtSpecies("", mutableStateOf(NbtState.new(IOUtils.readLevel(tree.level.readBytes()))))
                )
            } else {
                val pos = levelInfo
                    ?.getAs<CompoundTag>()?.get("Player")
                    ?.getAs<CompoundTag>()?.get("Pos")
                    ?.getAs<ListTag>()
                val x = pos?.get(0)?.getAs<DoubleTag>()?.value?.toInt()
                val z = pos?.get(2)?.getAs<DoubleTag>()?.value?.toInt()
                val extras = data.extras.toMutableMap()
                if (x != null && z != null) extras["playerpos"] = "$x, $z"
                MultipleSpeciesHolder(data.key, data.format, data.contentType, extras)
            }

        states.phylum.list.add(newHolder)
        states.phylum.species = newHolder
    }

    val categories = createCategories(tree)

    MaterialTheme {
        MainColumn {
            TopBar { TopBarText(name) }

            MainArea {
                MainFiles {
                    FileCategoryListScrollable(scrollState) {
                        for (category in categories) {
                            PhylumCategory(category) {
                                PhylumCategoryItems(
                                    category,
                                    states.phylum.species?.ident ?: "",
                                    onCategoryItemClick
                                )
                            }
                        }
                        CategoriesBottomMargin()
                    }
                    FileCategoryListScrollbar(scrollState)
                }
                MainContents {
                    if (states.phylum.list.size == 0) {
                        NoFileSelected(name)
                    } else {
                        val phylum = states.phylum.list.find { states.phylum.species?.ident == it.ident }
                            ?: return@MainContents

                        Editor(tree, phylum, true)
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
    tree: WorldTree,
    holder: SpeciesHolder,
    selected: Boolean
) {
    if (!selected) return

    EditorRoot {
        if (holder is MultipleSpeciesHolder) {
            SpeciesTabGroup(
                holder.species.map { TabData(holder.selected == it, it) },
                { holder.select(it) },
                { holder.remove(it) }
            )
            Editables {
                for (species in holder.species) {
                    if (species is SelectorSpecies && holder.selected == species) {
                        Selector(tree, holder)
                    } else if (species is NbtSpecies && holder.selected == species) {
                        EditableField(species)
                    }
                }
            }
        } else if (holder is SingleSpeciesHolder) {
            Editables { EditableField(holder.species) }
        }
    }
}

@Composable
fun BoxScope.Selector(
    tree: WorldTree,
    holder: MultipleSpeciesHolder
) {

    val onSelectChunk: (ChunkLocation, File?) -> Unit = select@ { loc, file ->
        if (file == null) return@select

        val newIdent = "[${loc.x}, ${loc.z}]"
        if (holder.hasSpecies(newIdent)) return@select

        val root = AnvilWorker.loadChunk(loc, file.readBytes()) ?: return@select

        holder.add(NbtSpecies(newIdent, mutableStateOf(NbtState.new(root))))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (holder.format == Species.Format.MCA) AnvilSelector(tree, holder, onSelectChunk)
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.AnvilSelector(
    tree: WorldTree,
    holder: MultipleSpeciesHolder,
    onSelectChunk: (ChunkLocation, File?) -> Unit
) {

    val dimension = holder.extra["dimension"] ?: return

    val worldDimension = tree[dimension]

    val selector = holder.species.find { it is SelectorSpecies } as SelectorSpecies
    val state = selector.state

    val anvils = when(holder.contentType) {
        Species.ContentType.TERRAIN -> worldDimension.region
        Species.ContentType.POI -> worldDimension.poi
        Species.ContentType.ENTITY -> worldDimension.entities
        else -> return
    }

    val chunks = mutableListOf<ChunkLocation>()
    anvils.forEach {
        val segments = it.name.split(".")
        val location = AnvilLocation(segments[1].toInt(), segments[2].toInt())
        chunks.addAll(AnvilWorker.loadChunkList(location, it.readBytes()))
    }

    if (state.initialComposition && chunks.isNotEmpty() && (state.selectedChunk == null || !chunks.contains(state.selectedChunk))) {
        state.selectedChunk = chunks[0]
    }

    if (state.chunkXValue.text == "") state.chunkXValue = TextFieldValue("${state.selectedChunk?.x ?: "-"}")
    if (state.chunkZValue.text == "") state.chunkZValue = TextFieldValue("${state.selectedChunk?.z ?: "-"}")

    val validChunkX = chunks.map { chunk -> chunk.x }
    val validChunkZ = chunks.map { chunk -> chunk.z }

    val updateSelectedChunk: () -> Unit = {
        val isValid = state.isChunkXValid && state.isChunkZValid
        val xInt = state.chunkXValue.text.toIntOrNull()
        val zInt = state.chunkZValue.text.toIntOrNull()
        val isInt = xInt != null && zInt != null
        // 이거 왜 스마트캐스팅 안해주더라?...
        val exists = if (isInt) chunks.contains(ChunkLocation(xInt!!, zInt!!)) else false
        state.selectedChunk =
            if (!isValid || !isInt || !exists) null
            else ChunkLocation(xInt!!, zInt!!)
    }

    val validate: (List<Int>, AnnotatedString) -> Pair<TransformedText, Boolean> = validate@ { valid, actual ->
        val int = actual.text.toIntOrNull()
        if (int == null) {
            TransformedText(
                AnnotatedString(actual.text, listOf(AnnotatedString.Range(SpanStyle(color = ThemedColor.Editor.Selector.Malformed), 0, actual.text.length))),
                OffsetMapping.Identity
            ) to false
        } else {
            if (valid.contains(int)) {
                TransformedText(AnnotatedString(actual.text), OffsetMapping.Identity) to true
            } else {
                TransformedText(
                    AnnotatedString(actual.text, listOf(AnnotatedString.Range(SpanStyle(color = ThemedColor.Editor.Selector.Invalid), 0, actual.text.length))),
                    OffsetMapping.Identity
                ) to false
            }
        }
    }

    val validateIntOnly: (AnnotatedString) -> TransformedText = { actual ->
        val int = actual.text.toIntOrNull()
        if (int == null) {
            TransformedText(
                AnnotatedString(actual.text, listOf(AnnotatedString.Range(SpanStyle(color = ThemedColor.Editor.Selector.Malformed), 0, actual.text.length))),
                OffsetMapping.Identity
            )
        } else {
            TransformedText(AnnotatedString(actual.text), OffsetMapping.Identity)
        }
    }

    val validateChunkX: (AnnotatedString) -> TransformedText = validate@ {
        val validateResult = validate(validChunkX, it)

        state.isChunkXValid = validateResult.second

        updateSelectedChunk()
        validateResult.first
    }

    val validateChunkZ: (AnnotatedString) -> TransformedText = validate@ {
        val validateResult = validate(validChunkZ, it)

        state.isChunkZValid = validateResult.second

        updateSelectedChunk()
        validateResult.first
    }

    val removeBlock: () -> Unit = {
        if (state.blockXValue.text != "-") state.blockXValue = TextFieldValue("-")
        if (state.blockZValue.text != "-") state.blockZValue = TextFieldValue("-")
    }

    val updateFromBlock: () -> Unit = {
        if (state.blockXValue.text.toIntOrNull() != null && state.blockZValue.text.toIntOrNull() != null) {
            val newChunk = BlockLocation(state.blockXValue.text.toInt(), state.blockZValue.text.toInt()).toChunkLocation()
            "${newChunk.x}".let { if (state.chunkXValue.text != it) state.chunkXValue = TextFieldValue(it) }
            "${newChunk.z}".let { if (state.chunkZValue.text != it) state.chunkZValue = TextFieldValue(it) }
            println("UPDATE FROM BLOCK")
            newChunk.let { if (state.selectedChunk != it) state.selectedChunk = it }
        }
    }

    if (state.selectedChunk?.toAnvilLocation() != null) {
        state.mapAnvil = state.selectedChunk?.toAnvilLocation()
    }

    var openPressed by remember { mutableStateOf(false) }
    var openFocused by remember { mutableStateOf(false) }

    if (state.initialComposition) state.initialComposition = false

    Row (
        modifier = Modifier
            .background(ThemedColor.SelectorArea)
            .height(60.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(10.dp))
        AnvilSelectorDropdown("block:") {
            CoordinateText("[")
            CoordinateInput(
                state.blockXValue,
                { state.blockXValue = it; updateFromBlock() },
                validateIntOnly
            )
            CoordinateText(", ")
            CoordinateInput(
                state.blockZValue,
                { state.blockZValue = it; updateFromBlock() },
                validateIntOnly
            )
            CoordinateText("]")
        }
        Spacer(modifier = Modifier.width(10.dp))
        AnvilSelectorDropdown("chunk:", true, state.selectedChunk != null) {
            CoordinateText("[")
            CoordinateInput(
                state.chunkXValue,
                { state.chunkXValue = it; removeBlock() },
                validateChunkX
            )
            CoordinateText(", ")
            CoordinateInput(
                state.chunkZValue,
                { state.chunkZValue = it; removeBlock() },
                validateChunkZ
            )
            CoordinateText("]")
        }
        Spacer(modifier = Modifier.width(10.dp))
        AnvilSelectorDropdown("region:") {
            val isChunkValid = state.isChunkXValid && state.isChunkZValid
            CoordinateText("r.")
            CoordinateText("${state.selectedChunk?.toAnvilLocation()?.x ?: "-"}", !isChunkValid)
            CoordinateText(".")
            CoordinateText("${state.selectedChunk?.toAnvilLocation()?.z ?: "-"}", !isChunkValid)
            CoordinateText(".mca")
        }
        Spacer(modifier = Modifier.weight(1f))
        Row (
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
                    if (chunk != null) {
                        val anvil = chunk.toAnvilLocation()
                        onSelectChunk(chunk, anvils.find { it.name == "r.${anvil.x}.${anvil.z}.mca" })
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
        if (state.mapAnvil != null)
        RegionPreview(tree, state.mapAnvil!!, state.selectedChunk) {
            state.chunkXValue = TextFieldValue(it.x.toString())
            state.chunkZValue = TextFieldValue(it.z.toString())
            state.blockXValue = TextFieldValue("-")
            state.blockZValue = TextFieldValue("-")
            state.selectedChunk = it
            // TODO: FATAL
            //  이거, 직접 확인하고 할당해야함.
            //  보여주는 지도는 Terrain 이지만, 만약 탭이 Terrain이 아닐 경우 데이터가 없는 청크일 수도 있음.
            state.isChunkXValid = true
            state.isChunkZValid = true
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BoxScope.EditableField(
    species: NbtSpecies,
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
