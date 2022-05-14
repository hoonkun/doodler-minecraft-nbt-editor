package composables.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.*
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
import composables.main.*
import composables.themed.*
import doodler.anvil.AnvilLocation
import doodler.anvil.AnvilManager
import doodler.anvil.BlockLocation
import doodler.anvil.ChunkLocation
import doodler.doodle.*
import keys
import kotlinx.coroutines.launch
import nbt.Tag
import nbt.TagType
import nbt.tag.ListTag
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.util.*


@Composable
fun MainColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), content = content)
}

@Composable
fun ColumnScope.TopBar(content: @Composable RowScope.() -> Unit) {
    TopAppBar(
        elevation = 0.dp,
        backgroundColor = Color(55, 55, 57),
        contentPadding = PaddingValues(start = 25.dp, top = 10.dp, bottom = 10.dp),
        modifier = Modifier
            .height(60.dp)
            .zIndex(1f)
            .drawBehind(border(bottom = Pair(1f, Color(30, 30, 30)))),
        content = content
    )
}

@Composable
fun RowScope.TopBarText(text: String) {
    Text(text, color = Color.White, fontSize = 25.sp)
}

@Composable
fun ColumnScope.MainArea(content: @Composable RowScope.() -> Unit) {
    Row (
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .background(Color(43, 43, 43))
            .zIndex(0f),
        content = content
    )
}

@Composable
fun RowScope.MainFiles(content: @Composable BoxScope.() -> Unit) {
    Box (modifier = Modifier.fillMaxHeight().weight(0.3f), content = content)
}

@Composable
fun BoxScope.FileCategoryListScrollable(scrollState: ScrollState, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(Color(60, 63, 65))
            .verticalScroll(scrollState),
        content = content
    )
}

@Composable
fun BoxScope.FileCategoryListScrollbar(scrollState: ScrollState) {
    VerticalScrollbar(
        ScrollbarAdapter(scrollState),
        style = ScrollbarStyle(
            100.dp,
            15.dp,
            RectangleShape,
            250,
            Color(255, 255, 255, 50),
            Color(255, 255, 255, 100)
        ),
        modifier = Modifier.align(Alignment.TopEnd)
    )
}

@Composable
fun RowScope.MainContents(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .weight(0.7f),
        content = content
    )
}

@Composable
fun ColumnScope.BottomBar(content: @Composable RowScope.() -> Unit) {
    BottomAppBar(
        elevation = 0.dp,
        backgroundColor = Color(60, 63, 65),
        contentPadding = PaddingValues(top = 0.dp, bottom = 0.dp, start = 25.dp, end = 25.dp),
        modifier = Modifier
            .height(40.dp)
            .zIndex(1f)
            .drawBehind(border(top = Pair(1f, Color(36, 36, 36)))),
        content = content
    )
}

@Composable
fun RowScope.BottomBarText(text: String) {
    Text(
        text,
        color = Color(255, 255, 255, 180),
        fontSize = 14.sp
    )
}

@Composable
fun ColumnScope.FileCategoryItems(
    parent: CategoryData,
    selected: String,
    onClick: (CategoryItemData) -> Unit
) {
    for (item in parent.items) {
        FileCategoryItem(item, selected, onClick)
    }
}

@Composable
fun ColumnScope.CategoriesBottomMargin() {
    Spacer(modifier = Modifier.height(25.dp))
}

@Composable
fun BoxScope.Editor(holder: EditableHolder) {
    EditorRoot {
        if (holder is MultipleEditableHolder) {
            TabGroup(
                holder.editables.map { TabData(holder.selected == it.ident, it) },
                { holder.select(it) },
                { holder.remove(it) }
            )
            Editables {
                for (editable in holder.editables) {
                    if (editable.ident == "+") {
                        if (holder.selected == editable.ident) {
                            Selector(holder, holder.selected == editable.ident)
                        }
                    } else {
                        if (editable.editorStateOrNull() == null)
                            editable.setEditorState(rememberEditorState())

                        if (holder.selected == editable.ident) {
                            EditableField(editable, editable.editorState)
                        }
                    }
                }
            }
        } else if (holder is SingleEditableHolder) {
            if (holder.editable.editorStateOrNull() == null)
                holder.editable.setEditorState(rememberEditorState())

            Editables { EditableField(holder.editable, holder.editable.editorState) }
        }
    }
}

@Composable
fun BoxScope.EditorRoot(content: @Composable ColumnScope.() -> Unit) {
    Column (
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f),
        content = content
    )
}

@Composable
fun ColumnScope.Editables(content: @Composable BoxScope.() -> Unit) {
    Box (
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        content = content
    )
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

    var selectedChunk by remember { mutableStateOf(if (chunks.isEmpty()) null else chunks[0]) }

    var chunkXValue by remember { mutableStateOf(TextFieldValue("${selectedChunk?.x ?: "-"}")) }
    var chunkZValue by remember { mutableStateOf(TextFieldValue("${selectedChunk?.z ?: "-"}")) }

    var blockXValue by remember { mutableStateOf(TextFieldValue("-")) }
    var blockZValue by remember { mutableStateOf(TextFieldValue("-")) }

    var isChunkXValid by remember { mutableStateOf(true) }
    var isChunkZValid by remember { mutableStateOf(true) }

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
                .onPointerEvent(PointerEventType.Enter, PointerEventPass.Final, updateOpenerEvtType)
                .onPointerEvent(PointerEventType.Exit, PointerEventPass.Final, updateOpenerEvtType)
                .onPointerEvent(PointerEventType.Press, PointerEventPass.Final, updateOpenerEvtType)
                .onPointerEvent(PointerEventType.Release, PointerEventPass.Final, updateOpenerEvtType)
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

@Composable
fun RowScope.CoordinateText(text: String, invalid: Boolean = false) {
    Text(
        text,
        fontSize = 21.sp,
        color = if (invalid) Color(100, 100, 100) else Color(169, 183, 198),
        fontFamily = JetBrainsMono,
        modifier = Modifier.focusable(false)
    )
}

@Composable
fun RowScope.CoordinateInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    transformer: (AnnotatedString) -> TransformedText = { TransformedText(it, OffsetMapping.Identity) }
) {
    BasicTextField(
        value,
        onValueChange,
        textStyle = TextStyle(
            fontSize = 21.sp,
            color = Color(169, 183, 198),
            fontFamily = JetBrainsMono
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        cursorBrush = SolidColor(Color(169, 183, 198)),
        visualTransformation = transformer,
        modifier = Modifier
            .width((value.text.length.coerceAtLeast(1) * 12.75).dp)
            .focusable(false)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowScope.AnvilSelectorDropdown(prefix: String, accent: Boolean = false, valid: Boolean = true, content: @Composable RowScope.() -> Unit) {
    Row (
        modifier = Modifier
            .background(
                if (accent && valid) Color(50, 54, 47)
                else if (accent) Color(64, 55, 52)
                else Color(42, 42, 42),
                RoundedCornerShape(4.dp)
            )
            .wrapContentWidth()
            .height(45.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(17.dp))
        Text(
            prefix,
            fontSize = 18.sp,
            color = Color(125, 125, 125),
            fontFamily = JetBrainsMono
        )
        Spacer(modifier = Modifier.width(10.dp))
        content()
        Spacer(modifier = Modifier.width(17.dp))
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
                        if (keys.contains(Key.CtrlLeft)) doodleState.addToSelected(item)
                        else if (keys.contains(Key.ShiftLeft)) {
                            val lastSelected = doodleState.getLastSelected()
                            if (lastSelected == null) doodleState.addToSelected(item)
                            else {
                                doodleState.addRangeToSelected(doodles.slice(
                                    doodles.indexOf(lastSelected) + 1 until doodles.indexOf(item) + 1
                                ))
                            }
                        } else doodleState.setSelected(item)
                    } else {
                        if (keys.contains(Key.CtrlLeft) || doodleState.selected.size == 1)
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

@Composable
fun BoxScope.NoFileSelected(worldName: String) {
    Column (
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            "< world >",
            color = Color(255, 255, 255, 100),
            fontSize = 29.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            worldName,
            color = Color.White,
            fontSize = 38.sp
        )
        Spacer(modifier = Modifier.height(35.dp))
        Text(
            "Select Tab in Left Area!",
            color = Color(255, 255, 255, 185),
            fontSize = 33.sp
        )
        Spacer(modifier = Modifier.height(25.dp))
        WhatIsThis("")
    }
}

@Composable
fun WhatIsThis(link: String) {
    Spacer(modifier = Modifier.height(60.dp))
    Text(
        "What is this?",
        color = Color(255, 255, 255, 145),
        fontSize = 25.sp
    )
    Spacer(modifier = Modifier.height(10.dp))
    LinkText(
        "Documentation",
        color = ThemedColor.Link,
        fontSize = 22.sp
    ) {
        openBrowser(link)
    }
}

fun openBrowser(url: String) {
    val osName = System.getProperty("os.name").lowercase(Locale.getDefault())

    val others: () -> Unit = {
        val runtime = Runtime.getRuntime()
        if (osName.contains("mac")) {
            runtime.exec("open $url")
        } else if (osName.contains("nix") || osName.contains("nux")) {
            runtime.exec("xdg-open $url")
        }
    }

    try {
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.BROWSE)) desktop.browse(URI(url))
            else others()
        } else {
            others()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
