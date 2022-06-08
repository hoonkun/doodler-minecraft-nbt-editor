package composable.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import doodler.doodle.structures.ActionDoodle
import doodler.doodle.structures.ReadonlyDoodle
import doodler.doodle.structures.TagDoodle
import doodler.editor.NbtEditor
import doodler.editor.states.NbtEditorState
import doodler.theme.DoodlerTheme
import doodler.types.Provider
import doodler.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun BoxScope.NbtEditor(
    editor: NbtEditor
) {

    val coroutine = rememberCoroutineScope()

    val toggle: (ReadonlyDoodle) -> Unit = toggle@ { doodle ->
        if (doodle !is TagDoodle || !doodle.tag.canHaveChildren) return@toggle

        if (doodle.expanded) doodle.collapse()
        else doodle.expand()
    }

    val select: (ReadonlyDoodle) -> Unit = {

    }

    val depthCollapse: (TagDoodle) -> Unit = { target ->
        target.collapse()

        val baseIndex = editor.state.items.indexOf(target)
        if (editor.state.lazyState.firstVisibleItemIndex > baseIndex)
            coroutine.launch { editor.state.lazyState.scrollToItem(baseIndex) }
    }

    val scrollTo: (ReadonlyDoodle) -> Unit = {
        coroutine.launch { editor.state.lazyState.scrollToItem(editor.state.items.indexOf(it)) }
    }

    TagDoodleDepthPreview(
        doodleProvider = { editor.state.focusedDepth?.let { Pair(it, editor.state.items.indexOf(it)) } },
        lazyStateProvider = { editor.state.lazyState },
        scrollTo = scrollTo
    )

    LazyColumn(state = editor.state.lazyState) {
        items(editor.state.items, key = { it.path }) { item ->
            when (item) {
                is ReadonlyDoodle ->
                    ReadonlyDoodle(
                        doodle = item,
                        toggle = toggle,
                        select = select,
                        collapse = depthCollapse,
                        selected = { editor.state.selected.contains(item) },
                        enabled = { editor.state.action != null },
                        actionTarget = { editor.state.action?.parent == item }
                    )
                is ActionDoodle ->
                    ActionDoodle(
                        doodle = item,
                        stateProvider = { editor.state }
                    )
            }
        }
    }

    LazyScrollbarDecoration(
        itemsProvider = { editor.state.items.filterIsInstance<ReadonlyDoodle>().toMutableStateList() },
        selectedProvider = { editor.state.selected },
        scrollTo = scrollTo
    )

    LazyScrollEffect(coroutine) { editor.state }

    Actions(stateProvider = { editor.state })

}

@Composable
fun LazyScrollEffect(
    coroutine: CoroutineScope,
    stateProvider: Provider<NbtEditorState>
) = SideEffect {
    val state = stateProvider()
    if (state.action != null)
        coroutine.launch { state.scrollToAction() }
}

@Composable
fun BoxScope.LazyScrollbarDecoration(
    itemsProvider: Provider<SnapshotStateList<ReadonlyDoodle>>,
    selectedProvider: Provider<SnapshotStateList<ReadonlyDoodle>>,
    scrollTo: (ReadonlyDoodle) -> Unit
) {
    val selected = selectedProvider()
    if (selected.isEmpty()) return

    val items = itemsProvider()

    val minSize = 3.dp
    val size = 1f / items.size
    val positionUnit = 1f / items.lastIndex

    Box(modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight().wrapContentWidth()) {
        for (item in items) {
            LazyScrollbarDecorationItem(
                size = size, minSize = minSize, position = positionUnit * items.indexOf(item),
                doodleProvider = { item },
                isSelectedProvider = { selected.contains(item) },
                scrollTo = scrollTo
            )
        }
    }

}

@Composable
fun BoxScope.LazyScrollbarDecorationItem(
    size: Float,
    minSize: Dp,
    position: Float,
    doodleProvider: Provider<ReadonlyDoodle>,
    isSelectedProvider: Provider<Boolean>,
    scrollTo: (ReadonlyDoodle) -> Unit
) {
    val hoverInteractionSource = remember { MutableInteractionSource() }
    val hovered by hoverInteractionSource.collectIsHoveredAsState()

    Column {
        if (position > 0) Spacer(modifier = Modifier.weight(position))

        Box(contentAlignment = Alignment.CenterEnd) {
            Canvas(
                modifier = Modifier
                    .fillMaxHeight(size).defaultMinSize(minSize).width(20.dp)
                    .hoverable(hoverInteractionSource)
            ) {
                drawRect(
                    if (isSelectedProvider()) DoodlerTheme.Colors.Editor.ScrollbarDecorSelected
                    else Color.Transparent,
                    size = this.size
                )
                drawRect(
                    if (!isSelectedProvider() || hovered) Color.Transparent
                    else DoodlerTheme.Colors.Background.copy(alpha = 0.35f),
                    size = this.size
                )
            }
            TagDoodlePreview(
                doodleProvider = doodleProvider,
                scrollTo = scrollTo,
                modifier = Modifier.wrapContentWidth().align(Alignment.TopEnd).offset(x = (-20).dp)
                    .hoverable(hoverInteractionSource)
            )
        }

        if (position < 1) Spacer(modifier = Modifier.weight(1 - position))
    }

}