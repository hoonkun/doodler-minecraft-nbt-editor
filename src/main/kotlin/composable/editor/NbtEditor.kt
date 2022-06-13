package composable.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import composable.rememberGlobalKeys
import doodler.doodle.structures.ActionDoodle
import doodler.doodle.structures.ReadonlyDoodle
import doodler.doodle.structures.TagDoodle
import doodler.editor.NbtEditor
import doodler.editor.states.NbtEditorState
import doodler.theme.DoodlerTheme
import doodler.types.Provider
import doodler.unit.ddp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BoxScope.NbtEditor(
    editor: NbtEditor
) {

    val coroutine = rememberCoroutineScope()

    val keys = rememberGlobalKeys()

    val toggle: (ReadonlyDoodle) -> Unit = toggle@ { doodle ->
        if (doodle !is TagDoodle || !doodle.tag.canHaveChildren) return@toggle

        if (doodle.expanded) doodle.collapse()
        else doodle.expand()
    }

    val select: (ReadonlyDoodle) -> Unit = select@ {
        if (keys.contains(Key.CtrlLeft) && keys.contains(Key.ShiftLeft)) return@select

        if (!editor.state.selected.contains(it)) {
            if (keys.contains(Key.CtrlLeft))
                editor.state.action { selector.multiSelectSingle(it) }
            else if (keys.contains(Key.ShiftLeft))
                editor.state.action { selector.multiSelectRange(it) }
            else
                editor.state.action { selector.select(it) }
        } else {
            if (keys.contains(Key.CtrlLeft) || keys.contains(Key.ShiftLeft) || editor.state.selected.size == 1)
                editor.state.action { selector.unselect(it) }
            else if (editor.state.selected.size > 1)
                editor.state.action { selector.select(it) }
        }
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
        doodleProvider = { editor.state.focused?.let { Pair(it, editor.state.items.indexOf(it)) } },
        lazyStateProvider = { editor.state.lazyState },
        focus = { editor.state.focused = it },
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
                        stateProvider = { editor.state },
                        selected = { editor.state.selected.contains(item) },
                        enabled = { editor.state.action == null },
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

    Log { editor.state.currentLog }

}

@Composable
fun LazyScrollEffect(
    coroutine: CoroutineScope,
    stateProvider: Provider<NbtEditorState>
) {
    val state = stateProvider()
    if (state.action != null)
        coroutine.launch { state.scrollToAction() }
}

@Composable
fun BoxScope.LazyScrollbarDecoration(
    itemsProvider: Provider<List<ReadonlyDoodle>>,
    selectedProvider: Provider<List<ReadonlyDoodle>>,
    scrollTo: (ReadonlyDoodle) -> Unit
) {
    val items = itemsProvider()
    val selected = selectedProvider()

    val minSize = 2.25.ddp
    val size = 1f / items.size
    val positionUnit = 1f / items.lastIndex

    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight().wrapContentWidth().zIndex(99f)
    ) {
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
    val indicatorHoverSource = remember { MutableInteractionSource() }
    val indicatorHovered by indicatorHoverSource.collectIsHoveredAsState()

    val previewHoverSource = remember { MutableInteractionSource() }
    val previewHovered by previewHoverSource.collectIsHoveredAsState()

    Row {

        if ((indicatorHovered || previewHovered) && isSelectedProvider()) {
            Column {
                if (position > 0) Spacer(modifier = Modifier.weight(position))

                TagDoodlePreview(
                    doodleProvider = doodleProvider,
                    scrollTo = scrollTo,
                    modifier = Modifier.wrapContentWidth().hoverable(previewHoverSource)
                )

                if (position < 1) Spacer(modifier = Modifier.weight(1 - position))
            }
        }

        Column {
            if (position > 0) Spacer(modifier = Modifier.weight(position))

            Canvas(
                modifier = Modifier
                    .fillMaxHeight(size).defaultMinSize(minSize).width(15.ddp)
                    .hoverable(indicatorHoverSource)
            ) {
                drawRect(
                    if (isSelectedProvider()) DoodlerTheme.Colors.Editor.ScrollbarDecorSelected
                    else Color.Transparent,
                    size = this.size
                )
                drawRect(
                    if (!isSelectedProvider() || indicatorHovered) Color.Transparent
                    else DoodlerTheme.Colors.Background.copy(alpha = 0.35f),
                    size = this.size
                )
            }

            if (position < 1) Spacer(modifier = Modifier.weight(1 - position))
        }

    }

}
