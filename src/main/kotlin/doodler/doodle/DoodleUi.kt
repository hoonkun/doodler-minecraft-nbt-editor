package doodler.doodle

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import doodler.extensions.contentEquals


typealias UiFunctionType = (ActualDoodle) -> Unit

@Stable
class DoodleUi {
    val selected = mutableStateListOf<ActualDoodle>()
    var pressed by mutableStateOf<ActualDoodle?>(null)
    var focusedDirectly by mutableStateOf<ActualDoodle?>(null)
    var focusedTree by mutableStateOf<ActualDoodle?>(null)
    var focusedTreeView by mutableStateOf<ActualDoodle?>(null)

    val press: UiFunctionType = { pressed = it }
    val release: UiFunctionType = { if (pressed == it) pressed = null }

    val directFocus: UiFunctionType = { if (focusedDirectly != it) focusedDirectly = it }
    val directBlur: UiFunctionType = { if (focusedDirectly == it) focusedDirectly = null }

    val treeFocus: UiFunctionType = { if (focusedTree != it) focusedTree = it }
    val treeBlur: (ActualDoodle?) -> Unit = { if (focusedTree == it || it == null) focusedTree = null }

    val treeViewFocus: UiFunctionType = { if (focusedTreeView != it) focusedTreeView = it }
    val treeViewBlur: UiFunctionType = { if (focusedTreeView == it) focusedTreeView = null }

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

}

@Stable
data class DoodleHierarchy(
    val parents: SnapshotStateList<NbtDoodle>,
    val parentUiStates: SnapshotStateList<DoodleItemUi>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DoodleHierarchy

        if (!parents.contentEquals(other.parents)) return false
        if (!parentUiStates.contentEquals(other.parentUiStates)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = parents.hashCode()
        result = 31 * result + parentUiStates.hashCode()
        return result
    }
}

fun Doodle.getHierarchy(state: DoodleUi): DoodleHierarchy {
    val result = mutableListOf<NbtDoodle>()
    var parent = if (this is ActualDoodle) parent else if (this is VirtualDoodle) parent else null
    while (parent != null && parent.depth >= 0) {
        result.add(parent)
        parent = parent.parent
    }
    result.reverse()
    return DoodleHierarchy(
        result.toMutableStateList(),
        result.map { state.toItemUi(it) }.toMutableStateList()
    )
}


@Stable
data class DoodleItemUi(
    val selected: Boolean,
    val pressed: Boolean,
    val focusedDirectly: Boolean,
    val focusedTree: Boolean,
    val focusedTreeView: Boolean,
    val functions: UiFunction,
    val hierarchy: DoodleHierarchy
)

@Stable
data class UiFunction(
    val press: UiFunctionType,
    val release: UiFunctionType,
    val directFocus: UiFunctionType,
    val directBlur: UiFunctionType,
    val treeFocus: UiFunctionType,
    val treeBlur: (ActualDoodle?) -> Unit,
    val treeViewFocus: UiFunctionType,
    val treeViewBlur: UiFunctionType,
)

fun DoodleUi.toItemUi(item: Doodle) =
    DoodleItemUi(
        selected = selected.contains(item),
        pressed = pressed == item,
        focusedDirectly = focusedDirectly == item,
        focusedTree = focusedTree == item,
        focusedTreeView = focusedTreeView == item,
        UiFunction(
            press, release, directFocus, directBlur, treeFocus, treeBlur, treeViewFocus, treeViewBlur
        ),
        item.getHierarchy(this)
    )
