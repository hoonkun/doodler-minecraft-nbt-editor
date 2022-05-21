package composables.states.editor.world

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue


class DoodleActionHistory(
    private val undoStack: MutableList<DoodleAction> = mutableStateListOf(),
    private val redoStack: MutableList<DoodleAction> = mutableStateListOf()
) {
    var flags by mutableStateOf(Flags(canBeUndo = undoStack.isNotEmpty(), canBeRedo = redoStack.isNotEmpty()))

    fun newAction(action: DoodleAction) {
        redoStack.clear()
        undoStack.add(action)
        updateFlags()
    }

    fun undo(): DoodleAction? {
        if (!flags.canBeUndo) return null
        redoStack.add(undoStack.removeLast())
        updateFlags()
        return redoStack.last()
    }

    fun redo(): DoodleAction? {
        if (!flags.canBeRedo) return null
        undoStack.add(redoStack.removeLast())
        updateFlags()
        return undoStack.last()
    }

    private fun updateFlags() {
        flags = Flags(canBeUndo = undoStack.isNotEmpty(), canBeRedo = redoStack.isNotEmpty())
    }

    class Flags (
        val canBeUndo: Boolean,
        val canBeRedo: Boolean
    )

}

abstract class DoodleAction

class DeleteDoodleAction(
    val deleted: List<ActualDoodle>
): DoodleAction()

class PasteDoodleAction(
    val created: List<ActualDoodle>
): DoodleAction()

class CreateDoodleAction(
    val created: ActualDoodle
): DoodleAction()

class EditDoodleAction(
    val old: ActualDoodle,
    val new: ActualDoodle
): DoodleAction()
