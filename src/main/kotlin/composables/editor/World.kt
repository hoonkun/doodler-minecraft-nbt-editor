package composables.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import composables.main.*
import composables.themed.*

@Composable
fun MainColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), content = content)
}

@Composable
fun ColumnScope.TopBar(content: @Composable RowScope.() -> Unit) {
    TopAppBar(
        elevation = 7.dp,
        backgroundColor = Color(55, 55, 57),
        contentPadding = PaddingValues(start = 25.dp, top = 15.dp, bottom = 15.dp),
        modifier = Modifier.zIndex(1f),
        content = content
    )
}

@Composable
fun RowScope.TopBarText(text: String) {
    Text(text, color = Color.White, fontSize = 32.sp)
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
        elevation = 20.dp,
        backgroundColor = Color(60, 63, 65),
        contentPadding = PaddingValues(top = 0.dp, bottom = 0.dp, start = 25.dp, end = 25.dp),
        modifier = Modifier.height(40.dp).zIndex(1f),
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
                        Editable(editable)
                    }
                }
            }
        } else if (holder is SingleEditableHolder) {
            Editables { Editable(holder.editable) }
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

@Composable
fun BoxScope.Editable(
    editable: Editable
) {

}

@Composable
fun BoxScope.NoFileSelected() {
    Column (
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            "World: //TODO", //TODO: World name from level.dat here.
            color = Color.White,
            fontSize = 38.sp,
        )
        Spacer(modifier = Modifier.height(25.dp))
        Text(
            "Select Tab in Left Area!",
            color = Color(255, 255, 255, 185),
            fontSize = 33.sp
        )
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
