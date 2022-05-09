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

    val editorFiles = remember { mutableStateMapOf<String, EditableHolder>() }
    var selectedFile by remember { mutableStateOf("") }

    val onCategoryItemClick: (CategoryItemData) -> Unit = lambda@ { data ->
        selectedFile = data.key

        if (editorFiles[selectedFile] != null) return@lambda

        editorFiles[data.key] = if (data.holderType == EditableHolder.Type.Single) {
            SingleEditableHolder(data.key, data.editableType, Editable(""))
        } else {
            MultipleEditableHolder(data.key, data.editableType)
        }
    }

    val generalItems: (String) -> List<CategoryItemData> = {
        listOf(
            CategoryItemData(it, "World Data", "level.dat", EditableHolder.Type.Single, Editable.Type.LEVEL_DAT),
            CategoryItemData(it, "Players", "playerdata/", EditableHolder.Type.Multiple, Editable.Type.PLAYER),
            CategoryItemData(it, "Statistics", "stats/", EditableHolder.Type.Multiple, Editable.Type.PLAYER),
            CategoryItemData(it, "Advancements", "advancements/", EditableHolder.Type.Multiple, Editable.Type.PLAYER),
        )
    }

    val dimensionItems: (String) -> List<CategoryItemData> = {
        val holderType = EditableHolder.Type.Multiple
        val editableType = Editable.Type.COMPRESSED_ANVIL
        val prefix = display(it)
        listOf(
            CategoryItemData(prefix, "Terrain", "region/", holderType, editableType),
            CategoryItemData(prefix, "Entities", "entities/", holderType, editableType),
            CategoryItemData(prefix, "Work Block Owners", "poi/", holderType, editableType),
            CategoryItemData(prefix, "Others", "data/", holderType, editableType),
        )
    }

    val categories = listOf(
        CategoryData("General", false, generalItems("General")),
        CategoryData(display(""), false, dimensionItems("")).withDescription(""),
        CategoryData(display("DIM-1"), true, dimensionItems("DIM-1")).withDescription("DIM-1"),
        CategoryData(display("DIM1"), true, dimensionItems("DIM1")).withDescription("DIM1")
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

                    for ((_, holder) in editorFiles) {
                        Editor(holder, selectedFile == holder.which)
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

class Editable(
    val ident: String
) {
    val hasChanges = false

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Editable) return false

        return other.ident == this.ident
    }

    override fun hashCode(): Int {
        return this.ident.hashCode()
    }

    enum class Type {
        LEVEL_DAT, PLAYER, COMPRESSED_ANVIL
    }
}

abstract class EditableHolder(
    val which: String,
    val type: Editable.Type
) {
    enum class Type {
        Single, Multiple
    }
}

class SingleEditableHolder(
    which: String,
    type: Editable.Type,
    editable: Editable
): EditableHolder(which, type) {
    val editable by mutableStateOf(editable)
}

class MultipleEditableHolder(
    which: String,
    type: Editable.Type
): EditableHolder(which, type) {
    val editables = mutableStateListOf<Editable>()
    var selected by mutableStateOf("+")

    init {
        editables.add(Editable("+"))
    }

    fun select(editable: Editable) {
        if (!editables.any { it.ident == editable.ident })
            return

        selected = editable.ident
    }

    fun add(editable: Editable) {
        if (!editables.any { it == editable || it.ident == editable.ident })
            editables.add(editable)
    }

    fun remove(editable: Editable) {
        if (editable.ident == selected)
            selected = editables[editables.indexOf(editable) - 1].ident

        editables.remove(editable)
    }
}
