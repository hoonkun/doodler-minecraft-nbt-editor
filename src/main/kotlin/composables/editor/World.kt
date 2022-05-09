package composables.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import composables.themed.CategoryItem

class WorldEditorComposable {
    companion object {

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
        fun RowScope.MainTabs(content: @Composable BoxScope.() -> Unit) {
            Box (modifier = Modifier.fillMaxHeight().weight(0.3f), content = content)
        }

        @Composable
        fun BoxScope.TabListScrollable(scrollState: ScrollState, content: @Composable ColumnScope.() -> Unit) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(Color(60, 63, 65))
                    .verticalScroll(scrollState),
                content = content
            )
        }

        @Composable
        fun BoxScope.TabListScrollbar(scrollState: ScrollState) {
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
        fun ColumnScope.GeneralItems(
            items: List<Pair<String, String>>,
            selected: String,
            onClick: (String) -> Unit
        ) {
            val category = "General"

            for ((name, path) in items) {
                CategoryItem(name, path, category, selected, onClick = onClick)
            }
        }

        @Composable
        fun ColumnScope.DimensionItems(
            items: List<Pair<String, String>>,
            selected: String,
            dimension: String,
            onClick: (String) -> Unit
        ) {
            for ((name, path) in items) {
                CategoryItem(name, path, dimension, selected, onClick = onClick)
            }
        }

    }
}