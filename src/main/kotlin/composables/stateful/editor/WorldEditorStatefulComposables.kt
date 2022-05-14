package composables.stateful.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.mouseClickable
import androidx.compose.foundation.rememberScrollState
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
import composables.main.*
import composables.stateless.editor.*
import composables.states.holder.rememberWorldEditorState
import composables.themed.*
import doodler.anvil.AnvilLocation
import doodler.anvil.AnvilManager
import doodler.anvil.BlockLocation
import doodler.anvil.ChunkLocation
import doodler.doodle.Doodle
import doodler.doodle.NbtDoodle
import doodler.doodle.doodle
import doodler.file.LevelData
import doodler.file.WorldDirectory
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
        states.worldSpec.tree = WorldDirectory.load(worldPath)

    if (states.worldSpec.name == null)
        states.worldSpec.name = LevelData.read(states.worldSpec.requireTree.level.readBytes())["Data"]
            ?.getAs<CompoundTag>()!!["LevelName"]
            ?.getAs<StringTag>()
            ?.value

    rootWorldData = states.worldSpec.requireTree

    if (states.worldSpec.tree == null || states.worldSpec.name == null) {
        // TODO: Handle Loading or Parse Error here
        return
    }

    val tree = states.worldSpec.requireTree
    val name = states.worldSpec.requireName

    val onCategoryItemClick: (CategoryItemData) -> Unit = lambda@ { data ->
        states.phylum.list.find { it.which == data.key }?.let {
            states.phylum.current = it
            return@lambda
        }

        val newHolder =
            if (data.holderType == EditableHolder.Type.Single) {
                SingleEditableHolder(
                    data.key, data.format, data.contentType,
                    Editable("", LevelData.read(tree.level.readBytes()))
                )
            } else {
                MultipleEditableHolder(data.key, data.format, data.contentType, data.extra)
            }

        states.phylum.list.add(newHolder)
        states.phylum.current = newHolder
    }

    val generalItems: (String) -> List<CategoryItemData> = {
        listOf(
            CategoryItemData(it, EditableHolder.Type.Single, Editable.Format.DAT, Editable.ContentType.LEVEL),
            CategoryItemData(it, EditableHolder.Type.Multiple, Editable.Format.DAT, Editable.ContentType.PLAYER),
            CategoryItemData(it, EditableHolder.Type.Multiple, Editable.Format.DAT, Editable.ContentType.STATISTICS),
            CategoryItemData(it, EditableHolder.Type.Multiple, Editable.Format.DAT, Editable.ContentType.ADVANCEMENTS)
        )
    }

    val dimensionItems: (String) -> List<CategoryItemData> = {
        val holderType = EditableHolder.Type.Multiple
        val prefix = display(it)
        val result = mutableListOf<CategoryItemData>()
        val extra = mapOf("dimension" to it)

        if (tree[it].region.isNotEmpty())
            result.add(CategoryItemData(prefix, holderType, Editable.Format.MCA, Editable.ContentType.TERRAIN, extra))
        if (tree[it].entities.isNotEmpty())
            result.add(CategoryItemData(prefix, holderType, Editable.Format.MCA, Editable.ContentType.ENTITY, extra))
        if (tree[it].poi.isNotEmpty())
            result.add(CategoryItemData(prefix, holderType, Editable.Format.MCA, Editable.ContentType.POI, extra))
        if (tree[it].data.isNotEmpty())
            result.add(CategoryItemData(prefix, holderType, Editable.Format.DAT, Editable.ContentType.OTHERS, extra))

        result
    }

    val categories = listOf(
        CategoryData("General", false, generalItems("General")),
        CategoryData(display(""), false, dimensionItems("")).withDescription(""),
        CategoryData(display("DIM-1"), true, dimensionItems("DIM-1")).withDescription("DIM-1"),
        CategoryData(display("DIM1"), true, dimensionItems("DIM1")).withDescription("DIM1")
    )

    MaterialTheme {
        MainColumn {
            TopBar { TopBarText(name) }

            MainArea {
                MainFiles {
                    FileCategoryListScrollable(scrollState) {
                        for (category in categories) {
                            FilesCategory(category) {
                                FileCategoryItems(
                                    category,
                                    states.phylum.current?.which ?: "",
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
                        for (phylum in states.phylum.list) {
                            Editor(phylum, states.phylum.current?.which == phylum.which)
                        }
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
fun BoxScope.Editor(holder: EditableHolder, selected: Boolean) {
    if (holder is MultipleEditableHolder) {
        if (holder.selectorStateOrNull() == null)
            holder.setSelectorState(rememberSelectorState())

        for (editable in holder.editables) {
            if (editable.ident == "+") continue

            if (editable.editorStateOrNull() != null) continue
            editable.setEditorState(rememberEditorState())
        }
    } else if (holder is SingleEditableHolder) {
        if (holder.editable.editorStateOrNull() == null)
            holder.editable.setEditorState(rememberEditorState())
    }

    if (!selected) return

    EditorRoot {
        if (holder is MultipleEditableHolder) {
            TabGroup(
                holder.editables.map { TabData(holder.selected == it.ident, it) },
                { holder.select(it) },
                { holder.remove(it) }
            )
            Editables {
                for (editable in holder.editables) {
                    if (editable.ident == "+" && holder.selected == editable.ident) {
                        Selector(holder, holder.selected == editable.ident)
                    } else if (holder.selected == editable.ident) {
                        EditableField(editable, editable.editorState)
                    }
                }
            }
        } else if (holder is SingleEditableHolder) {
            Editables { EditableField(holder.editable, holder.editable.editorState) }
        }
    }
}

@Composable
fun BoxScope.Selector(holder: MultipleEditableHolder, selected: Boolean) {

    val onSelectChunk: (ChunkLocation, File?) -> Unit = select@ { loc, file ->
        if (file == null) return@select

        val newIdent = "[${loc.x}, ${loc.z}]"
        if (holder.hasEditable(newIdent)) return@select

        val root = AnvilManager.instance.loadChunk(loc, file.readBytes())
        holder.add(Editable(newIdent, root))
    }

    Column {
        if (holder.format == Editable.Format.MCA) AnvilSelector(holder, onSelectChunk)
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
                holder.which,
                color = Color.White,
                fontSize = 38.sp,
            )
            Spacer(modifier = Modifier.height(25.dp))
            Text(
                "No files opened",
                color = Color(255, 255, 255, 185),
                fontSize = 33.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            LinkText(
                "Select File",
                color = ThemedColor.Bright,
                fontSize = 30.sp
            ) {
                holder.add(Editable("Some Name"))
            }
            WhatIsThis("")
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.AnvilSelector(
    holder: MultipleEditableHolder,
    onSelectChunk: (ChunkLocation, File?) -> Unit
) {

    val worldData = rootWorldData ?: return
    val dimension = holder.extra["dimension"] ?: return

    val worldDimension = worldData[dimension]

    val anvils = when(holder.contentType) {
        Editable.ContentType.TERRAIN -> worldDimension.region
        Editable.ContentType.POI -> worldDimension.poi
        Editable.ContentType.ENTITY -> worldDimension.entities
        else -> return
    }

    val chunks = mutableListOf<ChunkLocation>()
    anvils.forEach {
        val segments = it.name.split(".")
        val location = AnvilLocation(segments[1].toInt(), segments[2].toInt())
        chunks.addAll(AnvilManager.instance.loadChunkList(location, it.readBytes()))
    }

    val state = holder.selectorState

    var selectedChunk by state.selectedChunk

    if (selectedChunk == null && chunks.isNotEmpty())
        selectedChunk = chunks[0]

    var chunkXValue by state.chunkXValue
    var chunkZValue by state.chunkZValue

    if (chunkXValue.text == "") chunkXValue = TextFieldValue("${selectedChunk?.x ?: "-"}")
    if (chunkZValue.text == "") chunkZValue = TextFieldValue("${selectedChunk?.z ?: "-"}")

    var blockXValue by state.blockXValue
    var blockZValue by state.blockZValue

    var isChunkXValid by state.isChunkXValid
    var isChunkZValid by state.isChunkZValid

    val validChunkX = chunks.map { chunk -> chunk.x }
    val validChunkZ = chunks.map { chunk -> chunk.z }

    val updateSelectedChunk: () -> Unit = {
        selectedChunk =
            if (!isChunkXValid || !isChunkZValid || chunkXValue.text.toIntOrNull() == null || chunkZValue.text.toIntOrNull() == null) null
            else ChunkLocation(chunkXValue.text.toInt(), chunkZValue.text.toInt())
    }

    val validate: (List<Int>, AnnotatedString) -> Pair<TransformedText, Boolean> = validate@ { valid, actual ->
        val int = actual.text.toIntOrNull()
        if (int == null) {
            TransformedText(
                AnnotatedString(actual.text, listOf(AnnotatedString.Range(SpanStyle(color = Color(100, 100, 100)), 0, actual.text.length))),
                OffsetMapping.Identity
            ) to false
        } else {
            if (valid.contains(int)) {
                TransformedText(AnnotatedString(actual.text), OffsetMapping.Identity) to true
            } else {
                TransformedText(
                    AnnotatedString(actual.text, listOf(AnnotatedString.Range(SpanStyle(color = Color(230, 81, 0)), 0, actual.text.length))),
                    OffsetMapping.Identity
                ) to false
            }
        }
    }

    val validateIntOnly: (AnnotatedString) -> TransformedText = { actual ->
        val int = actual.text.toIntOrNull()
        if (int == null) {
            TransformedText(
                AnnotatedString(actual.text, listOf(AnnotatedString.Range(SpanStyle(color = Color(100, 100, 100)), 0, actual.text.length))),
                OffsetMapping.Identity
            )
        } else {
            TransformedText(AnnotatedString(actual.text), OffsetMapping.Identity)
        }
    }

    val validateChunkX: (AnnotatedString) -> TransformedText = validate@ {
        val validateResult = validate(validChunkX, it)

        isChunkXValid = validateResult.second

        updateSelectedChunk()
        validateResult.first
    }

    val validateChunkZ: (AnnotatedString) -> TransformedText = validate@ {
        val validateResult = validate(validChunkZ, it)

        isChunkZValid = validateResult.second

        updateSelectedChunk()
        validateResult.first
    }

    val removeBlock: () -> Unit = {
        blockXValue = TextFieldValue("-")
        blockZValue = TextFieldValue("-")
    }

    val updateFromBlock: () -> Unit = {
        if (blockXValue.text.toIntOrNull() != null && blockZValue.text.toIntOrNull() != null) {
            val newChunk = BlockLocation(blockXValue.text.toInt(), blockZValue.text.toInt()).toChunkLocation()
            chunkXValue = TextFieldValue("${newChunk.x}")
            chunkZValue = TextFieldValue("${newChunk.z}")
            selectedChunk = newChunk
        }
    }

    var openerEvtType by remember { mutableStateOf(PointerEventType.Release) }
    val updateOpenerEvtType: AwaitPointerEventScope.(PointerEvent) -> Unit = {
        openerEvtType = this.currentEvent.type
    }

    Row (
        modifier = Modifier
            .background(Color(36, 36, 36))
            .height(60.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(10.dp))
        AnvilSelectorDropdown("block:") {
            CoordinateText("[")
            CoordinateInput(
                blockXValue,
                { blockXValue = it; updateFromBlock() },
                validateIntOnly
            )
            CoordinateText(", ")
            CoordinateInput(
                blockZValue,
                { blockZValue = it; updateFromBlock() },
                validateIntOnly
            )
            CoordinateText("]")
        }
        Spacer(modifier = Modifier.width(10.dp))
        AnvilSelectorDropdown("chunk:", true, selectedChunk != null) {
            CoordinateText("[")
            CoordinateInput(
                chunkXValue,
                { chunkXValue = it; removeBlock() },
                validateChunkX
            )
            CoordinateText(", ")
            CoordinateInput(
                chunkZValue,
                { chunkZValue = it; removeBlock() },
                validateChunkZ
            )
            CoordinateText("]")
        }
        Spacer(modifier = Modifier.width(10.dp))
        AnvilSelectorDropdown("region:") {
            CoordinateText("r.")
            CoordinateText("${selectedChunk?.toAnvilLocation()?.x ?: "-"}", !isChunkXValid || !isChunkZValid)
            CoordinateText(".")
            CoordinateText("${selectedChunk?.toAnvilLocation()?.z ?: "-"}", !isChunkXValid || !isChunkZValid)
            CoordinateText(".mca")
        }
        Spacer(modifier = Modifier.weight(1f))
        Row (
            modifier = Modifier
                .background(
                    if (selectedChunk == null) {
                        Color.Transparent
                    } else {
                        when (openerEvtType) {
                            PointerEventType.Enter -> Color(255, 255, 255, 30)
                            PointerEventType.Press -> Color(255, 255, 255, 60)
                            else -> Color.Transparent
                        }
                    }
                )
                .onPointerEvent(PointerEventType.Enter, onEvent = updateOpenerEvtType)
                .onPointerEvent(PointerEventType.Exit, onEvent = updateOpenerEvtType)
                .onPointerEvent(PointerEventType.Press, onEvent = updateOpenerEvtType)
                .onPointerEvent(PointerEventType.Release, onEvent = updateOpenerEvtType)
                .height(60.dp)
                .width(60.dp)
                .alpha(if (selectedChunk == null) 0.5f else 1f)
                .mouseClickable {
                    val chunk = selectedChunk
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
                color = Color(175, 175, 175),
                fontFamily = JetBrainsMono
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BoxScope.EditableField(
    editable: Editable,
    state: EditorState
) {
    val nbt = editable.root ?: return

    val coroutineScope = rememberCoroutineScope()

    val doodles = state.doodles
    val doodleState = state.doodleState
    val lazyColumnState = state.lazyState

    if (doodles.isEmpty()) doodles.addAll(nbt.doodle(null, 0))

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

    Box {
        LazyColumn (state = lazyColumnState) {
            itemsIndexed(doodles, key = { _, item -> item.path }) { index, item ->
                val onExpand: () -> Unit = click@ {
                    if (item !is NbtDoodle) return@click
                    if (!item.hasChildren) return@click

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
                                doodleState.addRangeToSelected(doodles.slice(
                                    doodles.indexOf(lastSelected) + 1 until doodles.indexOf(item) + 1
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
                        .background(Color(255, 255, 255, 25), RoundedCornerShape(4.dp))
                        .wrapContentSize()
                        .onPointerEvent(PointerEventType.Move, onEvent = onToolBarMove)
                        .padding(5.dp)
                ) {
                    ToolBarAction {
                        IndicatorText("DEL", Color(227, 93, 48))
                    }
                    ToolBarAction {
                        IndicatorText("YNK", Color(88, 163, 126))
                    }
                    if (doodleState.selected.size == 1) {
                        val selectedDoodle = doodleState.selected[0]
                        if (selectedDoodle is NbtDoodle && !Tag.canHaveChildren(selectedDoodle.type)) {
                            ToolBarAction {
                                IndicatorText("EDT", Color(88, 132, 163))
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
                                .background(Color(255, 255, 255, 25), RoundedCornerShape(4.dp))
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
