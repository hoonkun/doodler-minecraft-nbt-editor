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
            SingleEditableHolder(data.key, data.format, data.contentType, Editable(""))
        } else {
            MultipleEditableHolder(data.key, data.format, data.contentType)
        }
    }

    val generalItems: (String) -> List<CategoryItemData> = {
        listOf(
            CategoryItemData(it, EditableHolder.Type.Single, Editable.Format.DAT, Editable.ContentType.LEVEL),
            CategoryItemData(it, EditableHolder.Type.Multiple, Editable.Format.DAT, Editable.ContentType.PLAYER),
            CategoryItemData(it, EditableHolder.Type.Multiple, Editable.Format.DAT, Editable.ContentType.STATISTICS),
            CategoryItemData(it, EditableHolder.Type.Multiple, Editable.Format.DAT, Editable.ContentType.ADVANCEMENTS),
        )
    }

    val dimensionItems: (String) -> List<CategoryItemData> = {
        val holderType = EditableHolder.Type.Multiple
        val prefix = display(it)
        listOf(
            CategoryItemData(prefix, holderType, Editable.Format.MCA, Editable.ContentType.TERRAIN),
            CategoryItemData(prefix, holderType, Editable.Format.MCA, Editable.ContentType.ENTITY),
            CategoryItemData(prefix, holderType, Editable.Format.MCA, Editable.ContentType.POI),
            CategoryItemData(prefix, holderType, Editable.Format.DAT, Editable.ContentType.OTHERS),
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

    enum class Format {
        DAT, MCA
    }

    enum class ContentType(val displayName: String, val description: String) {
        LEVEL("World Data", "level.dat"),
        PLAYER("Players", "playerdata/"),
        STATISTICS("Statistics", "stats/"),
        ADVANCEMENTS("Advancements", "advancements/"),
        OTHERS("Others", "data/"),

        TERRAIN("Terrain", "region/"),
        ENTITY("Entities", "entities/"),
        POI("Work Block Owners", "poi/")
    }
}

abstract class EditableHolder(
    val which: String,
    val format: Editable.Format,
    val contentType: Editable.ContentType
) {
    enum class Type {
        Single, Multiple
    }
}

class SingleEditableHolder(
    which: String,
    format: Editable.Format,
    contentType: Editable.ContentType,
    editable: Editable
): EditableHolder(which, format, contentType) {
    val editable by mutableStateOf(editable)
}

class MultipleEditableHolder(
    which: String,
    format: Editable.Format,
    contentType: Editable.ContentType
): EditableHolder(which, format, contentType) {
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
