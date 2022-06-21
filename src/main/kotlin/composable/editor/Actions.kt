package composable.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import doodler.doodle.structures.TagDoodle
import doodler.editor.states.NbtEditorState
import doodler.editor.structures.*
import doodler.extension.toRanges
import doodler.nbt.TagType
import doodler.theme.DoodlerTheme
import doodler.types.Provider
import doodler.unit.ddp

@Composable
fun BoxScope.Actions(
    stateProvider: Provider<NbtEditorState>
) {
    val state = stateProvider()
    if (state.action != null) return

    Row(modifier = Modifier.align(Alignment.TopEnd).padding(15.ddp)) {
        Column(modifier = Modifier.wrapContentSize().padding(end = 11.25.ddp)) {
            HistoryAction(state)
            ElevatorAction(state)
        }
        Column(modifier = Modifier.wrapContentSize()) {
            SaveAction(state)
            AlterAction(state)
            CreateAction(state)
        }
    }
}

@Composable
fun ActionSpacer() = Spacer(modifier = Modifier.height(15.ddp))

@Composable
fun ActionRoot(
    content: @Composable ColumnScope.() -> Unit
) = Column(
    modifier = Modifier
        .background(DoodlerTheme.Colors.Editor.ActionBackground, RoundedCornerShape(3.ddp))
        .wrapContentSize()
        .clickable {  }
        .padding(3.75.ddp),
    content = content
)

@Composable
fun ColumnScope.HistoryAction(
    state: NbtEditorState
) {
    ActionRoot {
        EditorActionButton(
            text = "UND",
            color = DoodlerTheme.Colors.Text.IdeGeneral,
            enabled = { state.actionFlags.canBeUndo },
            onClick = { state.action { history.undo() } }
        )
        EditorActionButton(
            text = "RED",
            color = DoodlerTheme.Colors.Text.IdeGeneral,
            enabled = { state.actionFlags.canBeRedo },
            onClick = { state.action { history.redo() } }
        )
    }
    ActionSpacer()
}

@Composable
fun ColumnScope.ElevatorAction(
    state: NbtEditorState
) {
    val available by derivedStateOf {
        state.selected.map { it.index }.toRanges().size == 1 && state.selected.map { it.parent }.toSet().size == 1
    }

    ActionRoot {
        EditorActionButton(
            text = "<- ",
            color = DoodlerTheme.Colors.Text.IdeGeneral,
            enabled = { available && (state.selected.firstOrNull()?.index ?: 0) != 0 },
            rotate = 90f to 1,
            onClick = { state.action { elevator.moveUp(state.selected) } }
        )
        EditorActionButton(
            text = " ->",
            color = DoodlerTheme.Colors.Text.IdeGeneral,
            enabled = {
                available && state.selected.lastOrNull()?.let { it.index == it.parent?.children?.lastIndex } != true
            },
            rotate = 90f to -1,
            onClick = { state.action { elevator.moveDown(state.selected) } }
        )
    }
}

@Composable
fun ColumnScope.SaveAction(
    state: NbtEditorState
) {
    val save = { state.save() }

    ActionRoot {
        EditorActionButton(
            text = "SAV",
            color = DoodlerTheme.Colors.DoodleAction.SaveAction,
            enabled = { true },
            onRightClick = save,
            onClick = save
        )
    }
    ActionSpacer()
}

@Composable
fun ColumnScope.AlterAction(
    state: NbtEditorState
) {
    val available by derivedStateOf { state.selected.isNotEmpty() }

    ActionRoot {
        EditorActionButton(
            text = "DEL",
            color = DoodlerTheme.Colors.DoodleAction.DeleteAction,
            enabled = { available },
            onClick = { state.action { deleter.delete() } }
        )
        EditorActionButton(
            text = "CPY",
            color = DoodlerTheme.Colors.Text.IdeGeneral,
            enabled = { available && state.actionFlags.canBeCopied },
            onClick = { state.action { clipboard.copy() } },
            onDisabledClick = { if (state.selected.size > 0) state.writeLog(CannotCopy()) }
        )
        EditorActionButton(
            text = "PST",
            color = DoodlerTheme.Colors.Text.IdeGeneral,
            enabled = { available && state.actionFlags.canBePasted },
            onClick = { state.action { clipboard.paste() } },
            onDisabledClick = { state.writeLog(CannotPaste(state)) }
        )
        EditorActionButton(
            text = "EDT",
            color = DoodlerTheme.Colors.Text.IdeGeneral,
            enabled = { available && state.actionFlags.canBeEdited },
            onClick = { state.action { editor.prepare() } },
            onDisabledClick = { if (state.selected.size > 1) state.writeLog(CannotEditMultipleItems()) }
        )
    }

    ActionSpacer()
}

@Composable
fun ColumnScope.CreateAction(
    state: NbtEditorState
) {
    val sortedTagTypes = remember {
        listOf(
            TagType.TAG_BYTE, TagType.TAG_SHORT, TagType.TAG_INT, TagType.TAG_LONG,
            TagType.TAG_FLOAT, TagType.TAG_DOUBLE,
            TagType.TAG_BYTE_ARRAY, TagType.TAG_INT_ARRAY, TagType.TAG_LONG_ARRAY, TagType.TAG_STRING,
            TagType.TAG_LIST, TagType.TAG_COMPOUND
        )
    }

    val available by derivedStateOf { state.selected.size <= 1 }

    val selected by derivedStateOf {
        if (state.selected.isEmpty()) state.root.tag
        else state.selected.first().let { if (it is TagDoodle) it.tag else null }
    }

    ActionRoot {
        for (tagType in sortedTagTypes) {
            TagCreatorButton(
                type = tagType,
                enabled = { available && selected?.canHold(tagType) == true },
                onClick = { state.action { creator.prepare(tagType) } },
                onDisabledClick = { state.writeLog(CannotCreate(tagType, state)) }
            )
        }
    }
}
