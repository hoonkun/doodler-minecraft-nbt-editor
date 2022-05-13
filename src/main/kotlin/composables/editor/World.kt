package composables.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import composables.main.*
import composables.themed.*
import doodler.doodle.*
import keys
import kotlinx.coroutines.launch

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
fun BoxScope.Editor(holder: EditableHolder, selected: Boolean) {
    EditorRoot(selected) {
        if (holder is MultipleEditableHolder) {
            TabGroup(
                holder.editables.map { TabData(holder.selected == it.ident, it) },
                { holder.select(it) },
                { holder.remove(it) }
            )
            Editables {
                for (editable in holder.editables) {
                    if (editable.ident == "+") {
                        Selector(holder, holder.selected == editable.ident)
                    } else {
                        EditableField(editable)
                    }
                }
            }
        } else if (holder is SingleEditableHolder) {
            Editables { EditableField(holder.editable) }
        }
    }
}

@Composable
fun BoxScope.EditorRoot(selected: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Column (
        modifier = Modifier
            .fillMaxSize()
            .alpha(if (selected) 1f else 0f)
            .zIndex(if (selected) 100f else -1f),
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
    Column (
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BoxScope.EditableField(
    editable: Editable
) {
    val nbt = editable.root ?: return

    val doodle = remember { mutableStateListOf(*nbt.doodle(null, 0).toTypedArray()) }

    val lazyColumnState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val doodleState = rememberDoodleState()

    val treeCollapse: (Doodle, Int) -> Unit = { target, collapseCount ->
        val baseIndex = doodle.indexOf(target)
        doodle.removeRange(baseIndex + 1, baseIndex + collapseCount + 1)
        if (lazyColumnState.firstVisibleItemIndex > baseIndex) {
            coroutineScope.launch { lazyColumnState.scrollToItem(baseIndex) }
        }
    }

    val treeViewTarget = if (doodleState.focusedTree == null) doodleState.focusedTreeView else doodleState.focusedTree
    if (treeViewTarget != null && lazyColumnState.firstVisibleItemIndex > doodle.indexOf(treeViewTarget)) {
        NbtItemTreeView(treeViewTarget, doodleState) {
            val index = doodle.indexOf(treeViewTarget)
            coroutineScope.launch {
                lazyColumnState.scrollToItem(index)
            }
            doodleState.unFocusTreeView(treeViewTarget)
            doodleState.unFocusTree(treeViewTarget)
            doodleState.focusDirectly(treeViewTarget)
        }
    }

    LazyColumn (state = lazyColumnState) {
        itemsIndexed(doodle, key = { _, item -> item.path }) { index, item ->
            val onExpand: () -> Unit = click@ {
                if (item !is NbtDoodle) return@click
                if (!item.hasChildren) return@click

                if (!item.expanded) doodle.addAll(index + 1, item.expand())
                else doodle.removeRange(index + 1, index + item.collapse() + 1)
            }
            val onSelect: () -> Unit = {
                if (!doodleState.selected.contains(item)) {
                    if (keys.contains(Key.CtrlLeft)) doodleState.addToSelected(item)
                    else if (keys.contains(Key.ShiftLeft)) {
                        val lastSelected = doodleState.getLastSelected()
                        if (lastSelected == null) doodleState.addToSelected(item)
                        else {
                            doodleState.addRangeToSelected(doodle.slice(
                                doodle.indexOf(lastSelected) + 1 until doodle.indexOf(item) + 1
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

    }
}
