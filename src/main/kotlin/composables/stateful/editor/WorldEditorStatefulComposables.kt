package composables.stateful.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import composables.states.holder.*
import composables.themed.*
import doodler.anvil.AnvilLocation
import doodler.anvil.AnvilWorker
import doodler.anvil.BlockLocation
import doodler.anvil.ChunkLocation
import doodler.file.WorldTree
import doodler.file.IOUtils
import keys
import kotlinx.coroutines.launch
import nbt.Tag
import nbt.TagType
import nbt.tag.CompoundTag
import nbt.tag.ListTag
import nbt.tag.StringTag
import java.io.File

@Composable
fun WorldEditor(
    worldPath: String
) {
    val scrollState = rememberScrollState()

    val states = rememberWorldEditorState()

    if (states.worldSpec.tree == null)
        states.worldSpec.tree = IOUtils.load(worldPath)

    if (states.worldSpec.name == null)
        states.worldSpec.name = IOUtils.readLevel(states.worldSpec.requireTree.level.readBytes())["Data"]
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
                    NbtSpecies("", IOUtils.readLevel(tree.level.readBytes()), mutableStateOf(NbtState.new()))
                )
            } else {
                MultipleSpeciesHolder(data.key, data.format, data.contentType, data.extras)
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
                        Selector(tree, holder, holder.selected == species)
                    } else if (species is NbtSpecies && holder.selected == species) {
                        EditableField(species, species.state)
                    }
                }
            }
        } else if (holder is SingleSpeciesHolder) {
            Editables { EditableField(holder.species, holder.species.state) }
        }
    }
}

@Composable
fun BoxScope.Selector(
    tree: WorldTree,
    holder: MultipleSpeciesHolder,
    selected: Boolean
) {

    val onSelectChunk: (ChunkLocation, File?) -> Unit = select@ { loc, file ->
        if (file == null) return@select

        val newIdent = "[${loc.x}, ${loc.z}]"
        if (holder.hasSpecies(newIdent)) return@select

        val root = AnvilWorker.loadChunk(loc, file.readBytes()) ?: return@select

        holder.add(NbtSpecies(newIdent, root, mutableStateOf(NbtState.new())))
    }

    Column {
        if (holder.format == Species.Format.MCA) AnvilSelector(tree, holder, onSelectChunk)
        Column (
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .alpha(if (selected) 1f else 0f)
                .zIndex(if (selected) 100f else -1f)
        ) {
            Text(
                holder.ident,
                color = Color.White,
                fontSize = 38.sp,
            )
            Spacer(modifier = Modifier.height(25.dp))
            Text(
                "No files opened",
                color = ThemedColor.WhiteSecondary,
                fontSize = 33.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            LinkText(
                "Select File",
                color = ThemedColor.Bright,
                fontSize = 30.sp
            ) {
//                holder.add(Species("Some Name"))
            }
            WhatIsThis("")
        }
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

    val selector = holder.species.find { it is SelectorSpecies } as SelectorSpecies
    val state = selector.state

    if (state.initialComposition && state.selectedChunk == null && chunks.isNotEmpty())
        state.selectedChunk = chunks[0]

    if (state.chunkXValue.text == "") state.chunkXValue = TextFieldValue("${state.selectedChunk?.x ?: "-"}")
    if (state.chunkZValue.text == "") state.chunkZValue = TextFieldValue("${state.selectedChunk?.z ?: "-"}")

    val validChunkX = chunks.map { chunk -> chunk.x }
    val validChunkZ = chunks.map { chunk -> chunk.z }

    val updateSelectedChunk: () -> Unit = {
        val isValid = state.isChunkXValid && state.isChunkZValid
        val isInt = state.chunkXValue.text.toIntOrNull() != null && state.chunkZValue.text.toIntOrNull() != null
        state.selectedChunk =
            if (!isValid || !isInt) null
            else ChunkLocation(state.chunkXValue.text.toInt(), state.chunkZValue.text.toInt())
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
            newChunk.let { if (state.selectedChunk != it) state.selectedChunk = it }
        }
    }

    var openPressed by remember { mutableStateOf(false) }
    var openFocused by remember { mutableStateOf(false) }

    state.initialComposition = false

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
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun BoxScope.EditableField(
    species: NbtSpecies,
    state: NbtState
) {
    val nbt = species.root

    val coroutineScope = rememberCoroutineScope()

    val doodles = state.doodles
    val doodleState = state.ui
    val lazyColumnState = state.lazyState

    if (doodles.isEmpty()) doodles.addAll(nbt.doodle(null, 0))

    if (state.initialComposition && nbt.value.size == 1 && nbt.value.values.toList()[0].canHaveChildren)
        doodles.addAll((doodles[0] as NbtDoodle).expand())

    val treeCollapse: (Doodle, Int) -> Unit = { target, collapseCount ->
        val baseIndex = doodles.indexOf(target)
        doodles.removeRange(baseIndex + 1, baseIndex + collapseCount + 1)
        if (lazyColumnState.firstVisibleItemIndex > baseIndex) {
            coroutineScope.launch { lazyColumnState.scrollToItem(baseIndex) }
        }
    }

    val treeViewTarget = if (doodleState.focusedTree == null) doodleState.focusedTreeView else doodleState.focusedTree
    if (treeViewTarget != null && lazyColumnState.firstVisibleItemIndex > doodles.indexOf(treeViewTarget)) {
        NbtItemTreeView(treeViewTarget, doodleState) {
            val index = doodles.indexOf(treeViewTarget)
            coroutineScope.launch {
                lazyColumnState.scrollToItem(index)
            }
            doodleState.unFocusTreeView(treeViewTarget)
            doodleState.unFocusTree(treeViewTarget)
            doodleState.focusDirectly(treeViewTarget)
        }
    }

    val onToolBarMove: AwaitPointerEventScope.(PointerEvent) -> Unit = {
        val target = doodleState.focusedDirectly
        if (target != null) doodleState.unFocusDirectly(target)
    }

    val deleteTag: MouseClickScope.() -> Unit = {
        val deleteInfo = doodleState.selected.map { it.delete() }
        deleteInfo.forEach {
            if (it == null) return@forEach

            val (parent, doodle, deletedCount) = it
            val start = doodles.indexOf(doodle)
            doodles.removeRange(start, start + deletedCount + 1)
            parent.update(NbtDoodle.UpdateTarget.VALUE)
        }
        doodleState.selected.clear()
    }

    state.initialComposition = false

    Box {
        LazyColumn (state = lazyColumnState) {
            itemsIndexed(doodles, key = { _, item -> item.path }) { index, item ->
                val onExpand: () -> Unit = click@ {
                    if (item !is NbtDoodle) return@click
                    if (!item.canHaveChildren) return@click

                    if (!item.expanded) doodles.addAll(index + 1, item.expand())
                    else doodles.removeRange(index + 1, index + item.collapse() + 1)
                }
                val onSelect: () -> Unit = {
                    if (!doodleState.selected.contains(item)) {
                        if (keys.contains(androidx.compose.ui.input.key.Key.CtrlLeft)) doodleState.addToSelected(item)
                        else if (keys.contains(androidx.compose.ui.input.key.Key.ShiftLeft)) {
                            val lastSelected = doodleState.getLastSelected()
                            if (lastSelected == null) doodleState.addToSelected(item)
                            else {
                                val from = doodles.indexOf(lastSelected)
                                val to = doodles.indexOf(item)
                                doodleState.addRangeToSelected(doodles.slice(
                                    if (from < to) from + 1 until to + 1
                                    else to until from
                                ))
                            }
                        } else doodleState.setSelected(item)
                    } else {
                        if (keys.contains(androidx.compose.ui.input.key.Key.CtrlLeft) || doodleState.selected.size == 1)
                            doodleState.removeFromSelected(item)
                        else if (doodleState.selected.size > 1)
                            doodleState.setSelected(item)
                    }
                }
                NbtItem(item, onSelect, onExpand, doodleState, treeCollapse)
            }
        }
        if (doodleState.selected.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.TopEnd)
                    .padding(40.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(ThemedColor.Editor.Action.Background, RoundedCornerShape(4.dp))
                        .wrapContentSize()
                        .onPointerEvent(PointerEventType.Move, onEvent = onToolBarMove)
                        .padding(5.dp)
                ) {
                    ToolBarAction (onClick = deleteTag) {
                        IndicatorText("DEL", ThemedColor.Editor.Action.Delete)
                    }
                    ToolBarAction {
                        IndicatorText("YNK", ThemedColor.Editor.Tag.General)
                    }
                    if (doodleState.selected.size == 1) {
                        val selectedDoodle = doodleState.selected[0]
                        if (selectedDoodle is NbtDoodle && (selectedDoodle.tag.name != null || !Tag.canHaveChildren(selectedDoodle.type))) {
                            ToolBarAction {
                                IndicatorText("EDT", ThemedColor.Editor.Tag.General)
                            }
                        }
                    }
                }
                if (doodleState.selected.size == 1) {
                    val selectedDoodle = doodleState.selected[0]
                    if (selectedDoodle is NbtDoodle && Tag.canHaveChildren(selectedDoodle.type)) {
                        val isType: (TagType) -> Boolean = { it == selectedDoodle.type }
                        val isListType: (TagType) -> Boolean = {
                            if (selectedDoodle.tag !is ListTag) false
                            else selectedDoodle.tag.elementsType == it || selectedDoodle.tag.elementsType == TagType.TAG_END
                        }
                        val isCompoundOrListType: (TagType) -> Boolean = {
                            isType(TagType.TAG_COMPOUND) || isListType(it)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Column(
                            modifier = Modifier
                                .background(ThemedColor.Editor.Action.Background, RoundedCornerShape(4.dp))
                                .wrapContentSize()
                                .onPointerEvent(PointerEventType.Move, onEvent = onToolBarMove)
                                .padding(5.dp)
                        ) {
                            if (isType(TagType.TAG_BYTE_ARRAY) || isCompoundOrListType(TagType.TAG_BYTE))
                                ToolBarIndicator(TagType.TAG_BYTE)

                            if (isCompoundOrListType(TagType.TAG_SHORT))
                                ToolBarIndicator(TagType.TAG_SHORT)

                            if (isType(TagType.TAG_INT_ARRAY) || isCompoundOrListType(TagType.TAG_INT))
                                ToolBarIndicator(TagType.TAG_INT)

                            if (isType(TagType.TAG_LONG_ARRAY) || isCompoundOrListType(TagType.TAG_LONG))
                                ToolBarIndicator(TagType.TAG_LONG)

                            if (isCompoundOrListType(TagType.TAG_FLOAT))
                                ToolBarIndicator(TagType.TAG_FLOAT)

                            if (isCompoundOrListType(TagType.TAG_DOUBLE))
                                ToolBarIndicator(TagType.TAG_DOUBLE)

                            if (isCompoundOrListType(TagType.TAG_BYTE_ARRAY))
                                ToolBarIndicator(TagType.TAG_BYTE_ARRAY)

                            if (isCompoundOrListType(TagType.TAG_INT_ARRAY))
                                ToolBarIndicator(TagType.TAG_INT_ARRAY)

                            if (isCompoundOrListType(TagType.TAG_LONG_ARRAY))
                                ToolBarIndicator(TagType.TAG_LONG_ARRAY)

                            if (isCompoundOrListType(TagType.TAG_STRING))
                                ToolBarIndicator(TagType.TAG_STRING)

                            if (isCompoundOrListType(TagType.TAG_LIST))
                                ToolBarIndicator(TagType.TAG_LIST)

                            if (isCompoundOrListType(TagType.TAG_COMPOUND))
                                ToolBarIndicator(TagType.TAG_COMPOUND)
                        }
                    }
                }
            }
        }
    }
}
