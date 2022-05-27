package doodler.doodle

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList


class DoodleUi (
    val selected: SnapshotStateList<ActualDoodle>,
    pressed: MutableState<ActualDoodle?>,
    focusedDirectly: MutableState<ActualDoodle?>,
    focusedTree: MutableState<ActualDoodle?>,
    focusedTreeView: MutableState<ActualDoodle?>
) {
    var pressed by pressed
    var focusedDirectly by focusedDirectly
    var focusedTree by focusedTree
    var focusedTreeView by focusedTreeView

    companion object {
        fun new(
            selected: SnapshotStateList<ActualDoodle> = mutableStateListOf(),
            pressed: MutableState<ActualDoodle?> = mutableStateOf(null),
            focusedDirectly: MutableState<ActualDoodle?> = mutableStateOf(null),
            focusedTree: MutableState<ActualDoodle?> = mutableStateOf(null),
            focusedTreeView: MutableState<ActualDoodle?> = mutableStateOf(null)
        ) = DoodleUi(
            selected, pressed, focusedDirectly, focusedTree, focusedTreeView
        )
    }

    fun press(target: ActualDoodle) {
        pressed = target
    }

    fun unPress(target: ActualDoodle) {
        if (pressed == target) pressed = null
    }

    fun getLastSelected(): ActualDoodle? = if (selected.isEmpty()) null else selected.last()

    fun addRangeToSelected(targets: List<ActualDoodle>) {
        selected.addAll(targets.filter { !selected.contains(it) })
    }

    fun addToSelected(target: ActualDoodle) {
        if (!selected.contains(target)) selected.add(target)
    }

    fun removeFromSelected(target: ActualDoodle) {
        if (selected.contains(target)) selected.remove(target)
    }

    fun setSelected(target: ActualDoodle) {
        selected.clear()
        selected.add(target)
    }

    fun focusDirectly(target: ActualDoodle) {
        if (focusedDirectly != target) focusedDirectly = target
    }

    fun unFocusDirectly(target: ActualDoodle) {
        if (focusedDirectly == target) focusedDirectly = null
    }

    fun focusTreeView(target: ActualDoodle) {
        if (focusedTreeView != target) focusedTreeView = target
    }

    fun unFocusTreeView(target: ActualDoodle) {
        if (focusedTreeView == target) focusedTreeView = null
    }

    fun focusTree(target: ActualDoodle) {
        if (focusedTree != target) focusedTree = target
    }

    fun unFocusTree(target: ActualDoodle) {
        if (focusedTree == target) focusedTree = null
    }
}
