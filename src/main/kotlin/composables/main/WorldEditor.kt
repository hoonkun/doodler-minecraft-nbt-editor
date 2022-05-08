package composables.main

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.BottomAppBar
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import composables.themed.*

@Composable
fun WorldEditor(
    worldPath: String
) {
    val scrollState = rememberScrollState()

    val editorTabs = remember { mutableStateMapOf<String, EditorTabBase>() }
    var selectedTab by remember { mutableStateOf("") }

    val onDimensionItemClick: (String, String) -> Unit = { dimension, name ->
        val key = "$dimension/$name"
        if (editorTabs[key] == null) {
            editorTabs += key to EditorTabWithSubTabs(key, EditorSubTab.Type.CHUNK)
        }

        selectedTab = key
    }

    MaterialTheme {
        Column (modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                elevation = 7.dp,
                backgroundColor = Color(55, 55, 57),
                contentPadding = PaddingValues(start = 25.dp, top = 15.dp, bottom = 15.dp),
                modifier = Modifier.zIndex(1f)
            ) {
                Text (
                    "Doodler: Minecraft NBT Editor",
                    color = Color.White,
                    fontSize = 32.sp
                )
            }

            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(43, 43, 43))
                    .zIndex(0f)
            ) {
                Box (modifier = Modifier.fillMaxHeight().weight(0.3f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(Color(60, 63, 65))
                            .verticalScroll(scrollState)
                    ) {
                        DimensionCategory("General", initialFolded = false) {
                            GeneralItems {  }
                        }
                        for (dimension in listOf("", "DIM-1", "DIM1")) {
                            DimensionCategory(display(dimension), dimension, initialFolded = dimension != "") {
                                DimensionSpecificItems(dimension, onDimensionItemClick)
                            }
                        }
                        Spacer(modifier = Modifier.height(25.dp))
                    }
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
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.7f)
                ) {
                    for (editorTab in editorTabs) {
                        val tab = editorTab.value
                        if (tab is EditorTabWithSubTabs && tab.subTabs.isEmpty()) {
                            EmptyEditorTab(tab, selectedTab == tab.name)
                        } else {
                            EditorTab(tab, selectedTab == tab.name)
                        }
                    }
                }
            }

            BottomAppBar(
                elevation = 20.dp,
                backgroundColor = Color(60, 63, 65),
                contentPadding = PaddingValues(top = 0.dp, bottom = 0.dp, start = 25.dp, end = 25.dp),
                modifier = Modifier.height(40.dp).zIndex(1f)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "by kiwicraft",
                    color = Color(255, 255, 255, 180),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun GeneralItems(onClick: (String) -> Unit) {
    DimensionItem("World Data", "level.dat", onClick = onClick)
    DimensionItem("Players", "playerdata/", onClick = onClick)
    DimensionItem("Statistics", "stats/", onClick = onClick)
    DimensionItem("Advancements", "advancements/", onClick = onClick)
}

@Composable
fun DimensionSpecificItems(dimension: String, onClick: (String, String) -> Unit) {
    val onDimensionClick: (String) -> Unit = { onClick(display(dimension), it) }
    DimensionItem("Terrain", "region/", onClick = onDimensionClick)
    DimensionItem("Entities", "entities/", onClick = onDimensionClick)
    DimensionItem("Work Block Owners", "poi/", onClick = onDimensionClick)
    DimensionItem("Others", "data/", onClick = onDimensionClick)
}

@Composable
fun EditorTab(tab: EditorTabBase, selected: Boolean) {
    Column (
        modifier = Modifier
            .fillMaxSize()
            .alpha(if (selected) 1f else 0f)
            .zIndex(if (selected) 100f else -1f)
    ) {
        if (tab is EditorTabWithSubTabs) TabGroup(tab.subTabs.map { TabData(true, it) })
        // TODO: display tab.selectedSubTab.content or tab.content
    }
}

@Composable
fun EmptyEditorTab(tab: EditorTabWithSubTabs, selected: Boolean) {
    Column (
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .alpha(if (selected) 1f else 0f)
            .zIndex(if (selected) 100f else -1f)
    ) {
        Text(
            tab.name,
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

        }
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
}

fun display(dimension: String): String {
    return when (dimension) {
        "" -> "Overworld"
        "DIM-1" -> "Nether"
        "DIM1" -> "TheEnd"
        else -> "Unknown"
    }
}

abstract class EditorTabBase(
    val name: String
)

class EditorTabWithSingleContent(
    name: String,
    val content: EditorTabContent
): EditorTabBase(name)

class EditorTabWithSubTabs(
    name: String,
    val subTabType: EditorSubTab.Type = EditorSubTab.Type.NONE
): EditorTabBase(name) {
    val subTabs = mutableStateListOf<EditorSubTab>()

    fun addSubTab(subTab: EditorSubTab) {
        if (!subTabs.any { it == subTab || it.name == subTab.name }) subTabs.add(subTab)
    }

    fun removeSubTab(subTab: EditorSubTab) {
        subTabs.remove(subTab)
    }
}

class EditorSubTab(
    val name: String,
    val content: EditorTabContent
) {

    enum class Type {
        NONE, PLAYER, CHUNK
    }
}

class EditorTabContent {
    val hasChanges: Boolean = false
}