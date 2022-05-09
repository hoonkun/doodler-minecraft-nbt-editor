package composables.main

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import composables.editor.*
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