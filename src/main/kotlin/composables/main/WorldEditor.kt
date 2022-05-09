package composables.main

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import composables.editor.WorldEditorComposable.Companion.BottomBar
import composables.editor.WorldEditorComposable.Companion.BottomBarText
import composables.editor.WorldEditorComposable.Companion.CategoryItems
import composables.editor.WorldEditorComposable.Companion.MainArea
import composables.editor.WorldEditorComposable.Companion.MainColumn
import composables.editor.WorldEditorComposable.Companion.MainContents
import composables.editor.WorldEditorComposable.Companion.MainTabs
import composables.editor.WorldEditorComposable.Companion.TabListScrollable
import composables.editor.WorldEditorComposable.Companion.TabListScrollbar
import composables.editor.WorldEditorComposable.Companion.TopBar
import composables.editor.WorldEditorComposable.Companion.TopBarText
import composables.themed.*

@Composable
fun WorldEditor(
    worldPath: String
) {
    val scrollState = rememberScrollState()

    val editorTabs = remember { mutableStateMapOf<String, EditorTabBase>() }
    var selectedTab by remember { mutableStateOf("") }

    val onGeneralItemClick: (CategoryItemData) -> Unit = lambda@ { data ->
        selectedTab = data.key

        if (editorTabs[data.key] != null) return@lambda

        editorTabs[data.key] = if (data.key == "General/World Data") {
            EditorTabWithSingleContent(data.key, EditorNbtContent())
        } else {
            EditorTabWithSubTabs(data.key, EditorTabWithSubTabs.Type.PLAYER)
        }
    }

    val onDimensionItemClick: (CategoryItemData) -> Unit = lambda@ { data ->
        selectedTab = data.key

        if (editorTabs[data.key] != null) return@lambda

        editorTabs += data.key to EditorTabWithSubTabs(data.key, EditorTabWithSubTabs.Type.CHUNK)
    }

    val createDimensionCategoryData: (String) -> CategoryData = { dimension ->
        CategoryData(display(dimension), dimension != "").withDescription(dimension)
    }

    val generalItems = listOf(
        Pair("World Data", "level.dat"),
        Pair("Players", "playerdata/"),
        Pair("Statistics", "stats/"),
        Pair("Advancements", "advancements/"),
    )

    val dimensionItems = listOf(
        Pair("Terrain", "region/"),
        Pair("Entities", "entities/"),
        Pair("Work Block Owners", "poi/"),
        Pair("Others", "data/"),
    )

    MaterialTheme {
        MainColumn {
            TopBar { TopBarText("Doodler: Minecraft NBT Editor") }

            MainArea {
                MainTabs {
                    TabListScrollable(scrollState) {
                        Category(CategoryData("General", false)) { category ->
                            CategoryItems(category, generalItems, selectedTab, onGeneralItemClick)
                        }
                        for (dimension in listOf("", "DIM-1", "DIM1")) {
                            Category(createDimensionCategoryData(dimension)) { category ->
                                CategoryItems(category, dimensionItems, selectedTab, onDimensionItemClick)
                            }
                        }
                        Spacer(modifier = Modifier.height(25.dp))
                    }
                    TabListScrollbar(scrollState)
                }
                MainContents {
                    if (editorTabs.size == 0) {
                        NoTabSelected()
                    }
                    for (editorTab in editorTabs) {
                        val tab = editorTab.value
                        EditorTab(tab, selectedTab == tab.name)
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
fun EditorTab(tab: EditorTabBase, selected: Boolean) {
    Column (
        modifier = Modifier
            .fillMaxSize()
            .alpha(if (selected) 1f else 0f)
            .zIndex(if (selected) 100f else -1f)
    ) {
        if (tab is EditorTabWithSubTabs) {
            TabGroup(tab.subTabs.map { TabData(tab.selected == it.name, it) }) { tab.select(it) }
            Box (
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                for (subTab in tab.subTabs) {
                    if (subTab is EditorSelectorSubTab) {
                        SelectorTab(tab, tab.selected == subTab.name)
                    } else {
                        NbtTab()
                    }
                }
            }
        } else {
            NbtTab()
        }
        // TODO: display tab.selectedSubTab.content or tab.content
    }
}

@Composable
fun NbtTab(

) {

}

@Composable
fun SelectorTab(tab: EditorTabWithSubTabs, selected: Boolean) {
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
            tab.addSubTab(EditorNbtSubTab("Chunk [0, 0]", tab, EditorNbtContent()))
        }
        WhatIsThis("")
    }
}

@Composable
fun NoTabSelected() {
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
    val content: EditorNbtContent
): EditorTabBase(name)

class EditorTabWithSubTabs(
    name: String,
    val type: Type = Type.NONE
): EditorTabBase(name) {
    companion object {
        const val SELECTOR_TAB_NAME = "+"
    }

    val subTabs = mutableStateListOf<EditorSubTabBase>()

    var selected by mutableStateOf("+")

    init {
        subTabs += EditorSelectorSubTab(SELECTOR_TAB_NAME, this)
    }

    fun select(what: String) {
        if (!subTabs.any { it.name == what }) return
        selected = what
    }

    fun addSubTab(subTab: EditorSubTabBase) {
        if (!subTabs.any { it == subTab || it.name == subTab.name }) subTabs.add(subTab)
    }

    fun removeSubTab(subTab: EditorSubTabBase) {
        if (subTab.name == selected) selected = subTabs[subTabs.indexOf(subTab) - 1].name
        subTabs.remove(subTab)
    }

    enum class Type {
        NONE, PLAYER, CHUNK
    }
}

abstract class EditorSubTabBase(
    val name: String,
    val parent: EditorTabWithSubTabs
) {
    open fun close() {
        parent.removeSubTab(this)
    }
}

class EditorNbtSubTab(
    name: String,
    parent: EditorTabWithSubTabs,
    val content: EditorNbtContent
): EditorSubTabBase(name, parent) {
    override fun close() {

        super.close()
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is EditorNbtSubTab) return false

        return other.name == this.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

class EditorSelectorSubTab(
    name: String,
    parent: EditorTabWithSubTabs
): EditorSubTabBase(name, parent) {

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is EditorNbtSubTab) return false

        return other.name == this.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

class EditorNbtContent {
    val hasChanges: Boolean = false
}