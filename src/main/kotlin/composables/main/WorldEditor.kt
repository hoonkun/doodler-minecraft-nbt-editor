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
import composables.editor.WorldEditorComposable.Companion.CategoriesBottomMargin
import composables.editor.WorldEditorComposable.Companion.FileCategoryItems
import composables.editor.WorldEditorComposable.Companion.MainArea
import composables.editor.WorldEditorComposable.Companion.MainColumn
import composables.editor.WorldEditorComposable.Companion.MainContents
import composables.editor.WorldEditorComposable.Companion.MainFiles
import composables.editor.WorldEditorComposable.Companion.FileCategoryListScrollable
import composables.editor.WorldEditorComposable.Companion.FileCategoryListScrollbar
import composables.editor.WorldEditorComposable.Companion.TopBar
import composables.editor.WorldEditorComposable.Companion.TopBarText
import composables.themed.*

@Composable
fun WorldEditor(
    worldPath: String
) {
    val scrollState = rememberScrollState()

    val editorFiles = remember { mutableStateMapOf<String, EditorFile>() }
    var selectedFile by remember { mutableStateOf("") }

    val onCategoryItemClick: (CategoryItemData) -> Unit = lambda@ { data ->
        selectedFile = data.key

        if (editorFiles[data.key] != null) return@lambda

        editorFiles[data.key] = if (data.contentType == EditorContentType.SINGLE_NBT) {
            NbtFile(data.key, EditorNbtContent())
        } else {
            CompressedNbtListFile(data.key, data.contentType)
        }
    }

    val generalItems = listOf(
        Triple("World Data", "level.dat", EditorContentType.SINGLE_NBT),
        Triple("Players", "playerdata/", EditorContentType.PLAYER),
        Triple("Statistics", "stats/", EditorContentType.PLAYER),
        Triple("Advancements", "advancements/", EditorContentType.PLAYER),
    )

    val dimensionItems = listOf(
        Triple("Terrain", "region/", EditorContentType.CHUNK_IN_COMPRESSED_FILE),
        Triple("Entities", "entities/", EditorContentType.CHUNK_IN_COMPRESSED_FILE),
        Triple("Work Block Owners", "poi/", EditorContentType.CHUNK_IN_COMPRESSED_FILE),
        Triple("Others", "data/", EditorContentType.CHUNK_IN_COMPRESSED_FILE),
    )

    val createDimensionCategoryData: (String) -> CategoryData = { dimension ->
        CategoryData(display(dimension), dimension != "", dimensionItems).withDescription(dimension)
    }

    val categories = listOf(
        CategoryData("General", false, generalItems),
        *listOf("", "DIM-1", "DIM1").map(createDimensionCategoryData).toTypedArray()
    )

    MaterialTheme {
        MainColumn {
            TopBar { TopBarText("Doodler: Minecraft NBT Editor") }

            MainArea {
                MainFiles {
                    FileCategoryListScrollable(scrollState) {
                        for (category in categories) {
                            FilesCategory(category) { FileCategoryItems(category, selectedFile, onCategoryItemClick) }
                        }
                        CategoriesBottomMargin()
                    }
                    FileCategoryListScrollbar(scrollState)
                }
                MainContents {
                    if (editorFiles.size == 0)
                        NoFileSelected()

                    for (editorTab in editorFiles) {
                        val tab = editorTab.value
                        Editor(tab, selectedFile == tab.name)
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
fun BoxScope.Editor(file: EditorFile, selected: Boolean) {
    Column (
        modifier = Modifier
            .fillMaxSize()
            .alpha(if (selected) 1f else 0f)
            .zIndex(if (selected) 100f else -1f)
    ) {
        if (file is CompressedNbtListFile) {
            TabGroup(file.tabs.map { TabData(file.selected == it.name, it) }) { file.select(it) }
            Box (
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                for (tab in file.tabs) {
                    if (tab is SelectorTab) {
                        SelectorTab(file, file.selected == tab.name)
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
fun SelectorTab(tab: CompressedNbtListFile, selected: Boolean) {
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
            tab.addTab(NbtTab("Chunk [0, 0]", tab, EditorNbtContent()))
        }
        WhatIsThis("")
    }
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

fun display(dimension: String): String {
    return when (dimension) {
        "" -> "Overworld"
        "DIM-1" -> "Nether"
        "DIM1" -> "TheEnd"
        else -> "Unknown"
    }
}

enum class EditorContentType {
    SINGLE_NBT, PLAYER, CHUNK_IN_COMPRESSED_FILE
}

abstract class EditorFile(
    val name: String
)

class NbtFile(
    name: String,
    val content: EditorNbtContent
): EditorFile(name)

class CompressedNbtListFile(
    name: String,
    val type: EditorContentType
): EditorFile(name) {
    companion object {
        const val SELECTOR_TAB_NAME = "+"
    }

    val tabs = mutableStateListOf<FileEditorTab>()

    var selected by mutableStateOf("+")

    init {
        tabs += SelectorTab(SELECTOR_TAB_NAME, this)
    }

    fun select(what: String) {
        if (!tabs.any { it.name == what }) return
        selected = what
    }

    fun addTab(tab: FileEditorTab) {
        if (!tabs.any { it == tab || it.name == tab.name }) tabs.add(tab)
    }

    fun removeTab(tab: FileEditorTab) {
        if (tab.name == selected) selected = tabs[tabs.indexOf(tab) - 1].name
        tabs.remove(tab)
    }
}

abstract class FileEditorTab(
    val name: String,
    val parent: CompressedNbtListFile
) {
    open fun close() {
        parent.removeTab(this)
    }
}

class NbtTab(
    name: String,
    parent: CompressedNbtListFile,
    val content: EditorNbtContent
): FileEditorTab(name, parent) {

    override fun close() {
        super.close()
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is NbtTab) return false

        return other.name == this.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

class SelectorTab(
    name: String,
    parent: CompressedNbtListFile
): FileEditorTab(name, parent) {

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is NbtTab) return false

        return other.name == this.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

class EditorNbtContent {
    val hasChanges: Boolean = false
}