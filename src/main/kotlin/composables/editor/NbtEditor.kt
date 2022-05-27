package composables.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import doodler.extensions.toRanges
import composables.global.*
import doodler.doodle.ActualDoodle
import doodler.doodle.NbtDoodle
import doodler.doodle.VirtualDoodle
import doodler.doodle.structures.CannotBePasted
import doodler.editor.*
import doodler.editor.states.NbtState
import keys
import kotlinx.coroutines.launch
import doodler.nbt.TagType

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BoxScope.NbtEditor(
    species: NbtEditor,
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
            SaveActionColumn(state)
            Spacer(modifier = Modifier.height(20.dp))
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
            NbtText("UND", ThemedColor.Editor.Tag.General, 16.sp)
        }
        NbtActionButton(disabled = !actions.history.canBeRedo, onClick = { actions.withLog { history.redo() } }) {
            NbtText("RED", ThemedColor.Editor.Tag.General, 16.sp)
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
            NbtText("<- ", ThemedColor.Editor.Tag.General, 16.sp, rotate = 90f, multiplier = 1)
        }
        NbtActionButton(
            disabled = !(available && canMoveDown),
            onClick = { state.actions.elevator.moveDown(state.ui.selected) }
        ) {
            NbtText(" ->", ThemedColor.Editor.Tag.General, 16.sp, rotate = 90f, multiplier = -1)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.SaveActionColumn(
    state: NbtState,
) {
    Column(
        modifier = Modifier
            .background(ThemedColor.Editor.Action.Background, RoundedCornerShape(4.dp))
            .wrapContentSize()
            .padding(5.dp)
    ) actionColumn@{
        NbtActionButton(
            disabled = state.actions.history.lastActionUid == state.lastSaveUid,
            onClick = { state.actions.withLog { state.save() } }
        ) {
            NbtText("SAV", ThemedColor.Editor.Action.Save, 16.sp)
        }
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
