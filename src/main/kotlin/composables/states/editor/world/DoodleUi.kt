package composables.states.editor.world

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList


class DoodleUi (
    val selected: SnapshotStateList<Doodle>,
    pressed: MutableState<Doodle?>,
    focusedDirectly: MutableState<Doodle?>,
    focusedTree: MutableState<Doodle?>,
    focusedTreeView: MutableState<Doodle?>
) {
    var pressed by pressed
    var focusedDirectly by focusedDirectly
    var focusedTree by focusedTree
    var focusedTreeView by focusedTreeView

    companion object {
        fun new(
            selected: SnapshotStateList<Doodle> = mutableStateListOf(),
            pressed: MutableState<Doodle?> = mutableStateOf(null),
            focusedDirectly: MutableState<Doodle?> = mutableStateOf(null),
            focusedTree: MutableState<Doodle?> = mutableStateOf(null),
            focusedTreeView: MutableState<Doodle?> = mutableStateOf(null)
        ) = DoodleUi(
            selected, pressed, focusedDirectly, focusedTree, focusedTreeView
        )
    }

    fun press(target: Doodle) {
        pressed = target
    }

    fun unPress(target: Doodle) {
        if (pressed == target) pressed = null
    }

    fun getLastSelected(): Doodle? = if (selected.isEmpty()) null else selected.last()

    fun addRangeToSelected(targets: List<Doodle>) {
        selected.addAll(targets.filter { !selected.contains(it) })
    }

    fun addToSelected(target: Doodle) {
        if (!selected.contains(target)) selected.add(target)
    }

    fun removeFromSelected(target: Doodle) {
        if (selected.contains(target)) selected.remove(target)
    }

    fun setSelected(target: Doodle) {
        selected.clear()
        selected.add(target)
    }

    fun focusDirectly(target: Doodle) {
        if (focusedDirectly != target) focusedDirectly = target
    }

    fun unFocusDirectly(target: Doodle) {
        if (focusedDirectly == target) focusedDirectly = null
    }

    fun focusTreeView(target: Doodle) {
        if (focusedTreeView != target) focusedTreeView = target
    }

    fun unFocusTreeView(target: Doodle) {
        if (focusedTreeView == target) focusedTreeView = null
    }

    fun focusTree(target: Doodle) {
        if (focusedTree != target) focusedTree = target
    }

    fun unFocusTree(target: Doodle) {
        if (focusedTree == target) focusedTree = null
    }
}
