package doodler.doodle

import androidx.compose.runtime.*


@Stable
class DoodleUi {
    val selected = mutableStateListOf<ActualDoodle>()
    var pressed by mutableStateOf<ActualDoodle?>(null)
    var focusedDirectly by mutableStateOf<ActualDoodle?>(null)
    var focusedTree by mutableStateOf<ActualDoodle?>(null)
    var focusedTreeView by mutableStateOf<ActualDoodle?>(null)

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
