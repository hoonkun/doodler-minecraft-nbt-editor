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
import doodler.minecraft.structures.WorldSpecification
import doodler.theme.DoodlerTheme
import doodler.types.Provider
import doodler.unit.ddp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BoxScope.NbtEditor(
    editor: NbtEditor,
    worldSpec: WorldSpecification? = null
) {

    val coroutine = rememberCoroutineScope()

    val keys = rememberGlobalKeys()

    val toggle: (ReadonlyDoodle) -> Unit = toggle@ { doodle ->
        if (doodle !is TagDoodle || !doodle.tag.canHaveChildren) return@toggle

        if (doodle.expanded) {
            val collapsed = doodle.collapse()
            editor.state.selected.removeAll(collapsed)
        } else doodle.expand()
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
        val collapsed = target.collapse()
        editor.state.selected.removeAll(collapsed)

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

    if (editor.state.action == null) {
        Actions(state = editor.state, worldSpec = worldSpec)
    }

    Log(editor.state.currentLog)

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


// TODO: 아무리 생각해도 이거 구현 방식이 바람직하지 못함.
//  이상적이려면 Canvas 하나에 모든 녹색 인디케이터를 그리고, 포인터 이벤트 같은걸로 어느 인디케이터 위에 있는지,
//  그 인디케이터가 가리키는 Doodle이 뭔지를 찾고 그것만 보여줘서 결론적으로 컴포저블이 딱 두개여야 함
//  하나하나 순회하면서 인디케이터를 그리지도 말고 이전에 만들어뒀던 Collection<Int>.toRanges() 함수를 잘 사용하자.
@Composable
fun BoxScope.LazyScrollbarDecoration(
    itemsProvider: Provider<List<ReadonlyDoodle>>,
    selectedProvider: Provider<List<ReadonlyDoodle>>,
    scrollTo: (ReadonlyDoodle) -> Unit
) {
    val items = itemsProvider()
    val selected = selectedProvider()

    val minSize = 4.ddp
    val size = 1f / items.size
    val positionUnit = 1f / items.lastIndex

    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight().wrapContentWidth().zIndex(99f)
    ) {
        for (item in selected) {
            LazyScrollbarDecorationItem(
                size = size, minSize = minSize, position = positionUnit * items.indexOf(item),
                doodleProvider = { item },
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
    scrollTo: (ReadonlyDoodle) -> Unit
) {
    val indicatorInteraction = remember { MutableInteractionSource() }
    val indicatorHovered by indicatorInteraction.collectIsHoveredAsState()

    val previewInteraction = remember { MutableInteractionSource() }
    val previewHovered by previewInteraction.collectIsHoveredAsState()

    Row {

        if ((indicatorHovered || previewHovered)) {
            Column {
                if (position > 0) Spacer(modifier = Modifier.weight(position))

                TagDoodlePreview(
                    doodleProvider = doodleProvider,
                    scrollTo = scrollTo,
                    modifier = Modifier.wrapContentWidth().hoverable(previewInteraction)
                )

                if (position < 1) Spacer(modifier = Modifier.weight(1 - position))
            }
        }

        Column {
            if (position > 0) Spacer(modifier = Modifier.weight(position))

            Canvas(
                modifier = Modifier
                    .requiredHeightIn(min = minSize).fillMaxHeight(size).width(10.ddp)
                    .hoverable(indicatorInteraction)
            ) {
                drawRect(
                    DoodlerTheme.Colors.Editor.ScrollbarDecorSelected,
                    size = this.size
                )
                drawRect(
                    if (indicatorHovered) Color.Transparent
                    else DoodlerTheme.Colors.Background.copy(alpha = 0.35f),
                    size = this.size
                )
            }

            if (position < 1) Spacer(modifier = Modifier.weight(1 - position))
        }

    }

}
