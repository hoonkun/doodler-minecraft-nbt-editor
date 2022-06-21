package doodler.editor.structures

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import doodler.doodle.extensions.displayName
import doodler.doodle.structures.ArrayValueDoodle
import doodler.doodle.structures.TagDoodle
import doodler.editor.states.NbtEditorState
import doodler.exceptions.InternalAssertionException
import doodler.nbt.AnyTag
import doodler.nbt.TagType
import doodler.nbt.tag.ListTag

enum class EditorLogLevel(
    val background: Color
) {
    Fatal(Color(0xff573f35)),
    Success(Color(0xff354757)),
    Warn(Color(0xff575236))
}

@Stable
data class EditorLog(
    val level: EditorLogLevel,
    val title: String,
    val summary: String?,
    val description: String?,
    val createdAt: Long = System.currentTimeMillis()
)

private fun CannotCreateDescription(type: TagType): String {
    val typeName = "\"${type.displayName()}\""
    val available = mutableListOf(TagType.TAG_COMPOUND).apply {
        if (type == TagType.TAG_BYTE || type == TagType.TAG_INT || type == TagType.TAG_LONG)
            add(type.parentArrayType())
    }.joinToString(" or ") { "\"${it.displayName()}\" tag" }
    val list = "\"${TagType.TAG_LIST.displayName()}\" tag whose element type is $typeName"

    return "$typeName tag only be created in $available, or $list."
}

private fun lines(vararg lines: String?): String = lines.filterNotNull().joinToString("\n")

fun CannotCreate(it: TagType, state: NbtEditorState): EditorLog {
    val current =
        if (state.selected.isEmpty() || state.selected.size > 1) null
        else {
            when (val target = state.selected.first()) {
                is ArrayValueDoodle -> null
                is TagDoodle -> "current selection is \"${target.tag.type.displayName()}\" tag."
            }
        }

    return EditorLog(
        level = EditorLogLevel.Warn,
        title = "Cannot Create",
        summary = null,
        description = lines(CannotCreateDescription(it), current)
    )
}

fun CannotEditMultipleItems() =
    EditorLog(
        level = EditorLogLevel.Warn,
        title = "Cannot Edit",
        summary = null,
        description = "Cannot edit multiple items at once."
    )

fun CannotCopy() =
    EditorLog(
        level = EditorLogLevel.Warn,
        title = "Cannot Copy",
        summary = null,
        description = lines(
            "To operate copy action, selection must contains",
            "only one of \"named\" or \"unnamed\", or \"array value\" tag."
        )
    )

fun CannotPaste(it: NbtEditorState): EditorLog {
    return if (it.actionFlags.isClipboardEmpty) CannotPasteEmptyClipboard
    else if (it.selected.isEmpty()) CannotPasteEmptySelection
    else if (it.selected.size > 1) CannotPasteMultipleItems
    else {
        when (val target = it.selected.first()) {
            is ArrayValueDoodle -> CannotPasteInvalidParent
            is TagDoodle -> {
                if (!target.tag.type.canHaveChildren()) CannotPasteInvalidParent
                else CannotPasteInvalidCriteria(target.tag, it.actionFlags.pasteCriteria)
            }
        }
    }
}

private val CannotPasteEmptyClipboard =
    EditorLog(
        level = EditorLogLevel.Warn,
        title = "Cannot Paste",
        summary = null,
        description = "Clipboard is empty."
    )

private val CannotPasteEmptySelection =
    EditorLog(
        level = EditorLogLevel.Warn,
        title = "Cannot Paste",
        summary = null,
        description = lines(
            "No target tag specified.",
            "Try to select some tag with right click."
        )
    )

private val CannotPasteMultipleItems =
    EditorLog(
        level = EditorLogLevel.Warn,
        title = "Cannot Paste",
        summary = null,
        description = "Cannot paste clipboard items into multiple tags at once."
    )

private val CannotPasteInvalidParent =
    EditorLog(
        level = EditorLogLevel.Warn,
        title = "Cannot Paste",
        summary = null,
        description = "Selected tag cannot own any child."
    )

private val CannotPasteInvalidCriteria: (AnyTag, PasteCriteria) -> EditorLog = lambda@ { current, criteria ->
    val currentName = when (current.type) {
        TagType.TAG_LIST -> "\"${current.type.displayName()}\", whose type is \"${(current as ListTag).elementsType}\""
        else -> "\"${current.type.displayName()}\""
    }

    val criteriaName = when (criteria) {
        CannotBePasted -> throw InternalAssertionException("PasteCriteria, except CannotBePasted", "CannotBePasted")
        CanBePastedIntoCompound -> "\"${TagType.TAG_COMPOUND.displayName()}\""
        is CanBePastedIntoList -> "\"${TagType.TAG_LIST.displayName()}\" whose element type is " +
                "\"${criteria.elementsType}\" or \"${TagType.TAG_END.displayName()}\""
        is CanBePastedIntoArray -> "\"${criteria.arrayTagType.displayName()}\""
    }

    EditorLog(
        level = EditorLogLevel.Warn,
        title = "Cannot Paste",
        summary = null,
        description = lines(
            "Current clipboard item can only be pasted into $criteriaName,",
            "but selected is $currentName"
        )
    )
}
