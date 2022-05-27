package doodler.editor.states

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import doodler.doodle.*
import doodler.doodle.structures.*
import doodler.extensions.removeRange
import doodler.extensions.toRanges
import doodler.nbt.TagType
import doodler.nbt.tag.*

class NbtState (
    val rootDoodle: NbtDoodle,
    ui: MutableState<DoodleUi>,
    val lazyState: LazyListState,
    val logs: SnapshotStateList<DoodleLog>
) {

    companion object {
        fun new(
            rootTag: CompoundTag,
            ui: MutableState<DoodleUi> = mutableStateOf(DoodleUi.new()),
            lazyState: LazyListState = LazyListState(),
            logs: SnapshotStateList<DoodleLog> = mutableStateListOf()
        ) = NbtState(NbtDoodle(rootTag, -1, -1), ui, lazyState, logs)
    }

    var ui by ui

    val doodles: DoodleManager = DoodleManager(rootDoodle)

    val actions = Actions()

    val currentLogState: MutableState<DoodleLog?> = mutableStateOf(null)
    var currentLog by currentLogState

    init {
        rootDoodle.expand()

        val hasOnlyChild = rootDoodle.expandedItems
            .let { children -> children.size == 1 && children[0].let { it is NbtDoodle && it.tag.canHaveChildren } }

        if (hasOnlyChild) (rootDoodle.expandedItems[0] as NbtDoodle).expand()
    }

    inner class Actions {

        val history = HistoryAction()

        val creator = CreateAction()

        val editor = EditAction()

        val deleter = DeleteAction()

        val clipboard = ClipboardAction()

        val elevator = MoveAction()

        fun withLog(action: Actions.() -> Unit) {
            try { action() }
            catch (exception: DoodleException) {
                val newLog = DoodleLog(DoodleLogLevel.FATAL, exception.title, exception.summary, exception.description)
                if (logs.size > 10) logs.removeFirst()
                logs.add(newLog)
                currentLog = newLog
                exception.printStackTrace()
            }
            catch (exception: Exception) {
                exception.printStackTrace()
            }
        }

    }

    inner class HistoryAction {

        private val undoStack: MutableList<DoodleAction> = mutableStateListOf()
        private val redoStack: MutableList<DoodleAction> = mutableStateListOf()

        var canBeUndo: Boolean by mutableStateOf(false)
        var canBeRedo: Boolean by mutableStateOf(false)

        fun newAction(action: DoodleAction) {
            redoStack.clear()
            undoStack.add(action)
            updateFlags()
        }

        fun undo() {
            if (!canBeUndo) return

            redoStack.add(undoStack.removeLast())
            updateFlags()

            when (val action = redoStack.last()) {
                is DeleteDoodleAction -> undoDelete(action)
                is PasteDoodleAction -> undoPaste(action)
                is CreateDoodleAction -> undoCreate(action)
                is EditDoodleAction -> undoEdit(action)
                is MoveDoodleAction -> undoMove(action)
            }
        }

        fun redo() {
            if (!canBeRedo) return

            undoStack.add(redoStack.removeLast())
            updateFlags()

            when (val action = undoStack.last()) {
                is DeleteDoodleAction -> redoDelete(action)
                is PasteDoodleAction -> redoPaste(action)
                is CreateDoodleAction -> redoCreate(action)
                is EditDoodleAction -> redoEdit(action)
                is MoveDoodleAction -> redoMove(action)
            }
        }

        private fun updateFlags() {
            canBeUndo = undoStack.isNotEmpty()
            canBeRedo = redoStack.isNotEmpty()
        }

        private fun undoPaste(action: PasteDoodleAction) {
            actions.deleter.internal.delete(action.created)
        }

        private fun undoDelete(action: DeleteDoodleAction) {
            actions.creator.internal.create(action.deleted)
        }

        private fun undoCreate(action: CreateDoodleAction) {
            actions.deleter.internal.delete(listOf(action.created))
        }

        private fun redoCreate(action: CreateDoodleAction) {
            actions.creator.internal.create(listOf(action.created))
        }

        private fun undoEdit(action: EditDoodleAction) {
            actions.editor.internal.edit(action.new, action.old)
        }

        private fun redoEdit(action: EditDoodleAction) {
            actions.editor.internal.edit(action.old, action.new)
        }

        private fun redoPaste(action: PasteDoodleAction) {
            actions.creator.internal.create(action.created)
        }

        private fun redoDelete(action: DeleteDoodleAction) {
            actions.deleter.internal.delete(action.deleted)
        }

        private fun undoMove(action: MoveDoodleAction) {
            if (action.direction == MoveDoodleAction.DoodleMoveDirection.UP) {
                actions.elevator.internal.moveDown(action.moved)
            } else if (action.direction == MoveDoodleAction.DoodleMoveDirection.DOWN) {
                actions.elevator.internal.moveUp(action.moved)
            }
        }

        private fun redoMove(action: MoveDoodleAction) {
            if (action.direction == MoveDoodleAction.DoodleMoveDirection.UP) {
                actions.elevator.internal.moveUp(action.moved)
            } else if (action.direction == MoveDoodleAction.DoodleMoveDirection.DOWN) {
                actions.elevator.internal.moveDown(action.moved)
            }
        }

    }

    inner class MoveAction {

        val internal = Internal()

        fun moveUp(targets: List<ActualDoodle>) {
            internal.moveUp(targets)

            actions.history.newAction(MoveDoodleAction(MoveDoodleAction.DoodleMoveDirection.UP, targets))
        }

        fun moveDown(targets: List<ActualDoodle>) {
            internal.moveDown(targets)

            actions.history.newAction(MoveDoodleAction(MoveDoodleAction.DoodleMoveDirection.DOWN, targets))
        }

        inner class Internal {

            fun moveUp(targets: List<ActualDoodle>) {
                move(targets) { it.first - 1 }
            }

            fun moveDown(targets: List<ActualDoodle>) {
                move(targets) { it.first + 1 }
            }

            private fun move(targets: List<ActualDoodle>, into: (IntRange) -> Int) {
                val targetsRange = targets.map { it.index }.toRanges()
                if (targetsRange.size > 1)
                    throw InvalidOperationException("Cannot move up", "Selection range is invalid. Only continuous range is available for this action.")
                if (targetsRange.isEmpty())
                    throw InvalidOperationException("Cannot move up", "No targets selected.")

                val range = targetsRange[0]

                val variants = targets.map { if (it is NbtDoodle) "nbt" else "value" }.toSet().toList()
                if (variants.size > 1)
                    throw InternalAssertionException(listOf(NbtDoodle::class.java.simpleName, ValueDoodle::class.java.simpleName), "Both")

                val parent = targets.first().parent ?: throw ParentNotFoundException()
                val parentTag = parent.tag

                parent.expandedItems.addAll(into(range), parent.expandedItems.removeRange(range))

                if (variants[0] == "nbt") {
                    when (parentTag) {
                        is CompoundTag -> {
                            val tag = parentTag.getAs<CompoundTag>()
                            tag.value.addAll(into(range), tag.value.removeRange(range))
                        }
                        is ListTag -> {
                            val tag = parentTag.getAs<ListTag>()
                            tag.value.addAll(into(range), tag.value.removeRange(range))
                        }
                        else -> {
                            throw InternalError("Cannot move up", "NbtDoodle's parent is not Compound or List.")
                        }
                    }
                } else {
                    when (parentTag) {
                        is ByteArrayTag -> {
                            val tag = parentTag.getAs<ByteArrayTag>()
                            val newList = tag.value.toMutableList()
                            newList.addAll(into(range), tag.value.toMutableList().removeRange(range))
                            tag.value = newList.toByteArray()
                        }
                        is IntArrayTag -> {
                            val tag = parentTag.getAs<IntArrayTag>()
                            val newList = tag.value.toMutableList()
                            newList.addAll(into(range), tag.value.toMutableList().removeRange(range))
                            tag.value = newList.toIntArray()
                        }
                        is LongArrayTag -> {
                            val tag = parentTag.getAs<LongArrayTag>()
                            val newList = tag.value.toMutableList()
                            newList.addAll(into(range), tag.value.toMutableList().removeRange(range))
                            tag.value = newList.toLongArray()
                        }
                        else -> {
                            throw InternalError("Cannot move up", "ValueDoodle's parent is not Array.")
                        }
                    }
                }

                parent.update(NbtDoodle.UpdateTarget.INDEX)
            }

        }

    }

    inner class ClipboardAction {

        val stack: SnapshotStateList<Pair<PasteTarget, List<ActualDoodle>>> = mutableStateListOf()
        val pasteTarget: PasteTarget get() = checkPasteTarget()

        private fun checkPasteTarget(): PasteTarget {
            val selected = ui.selected
            val attributes = mutableSetOf<TagAttribute>()
            val tagTypes = mutableSetOf<TagType>()

            attributes.addAll(
                selected.map {
                    if (it is ValueDoodle) TagAttribute.VALUE
                    else if (it is NbtDoodle && it.tag.name == null) TagAttribute.UNNAMED
                    else TagAttribute.NAMED
                }
            )

            tagTypes.addAll(selected.mapNotNull {
                when (it) {
                    is NbtDoodle -> it.tag.type
                    is ValueDoodle -> it.parent?.tag?.type
                }
            })

            if (attributes.size != 1) return CannotBePasted
            val attribute = attributes.toList()[0]

            return if (attribute == TagAttribute.NAMED) {
                CanBePastedIntoCompound
            } else if (attribute == TagAttribute.UNNAMED && tagTypes.size == 1) {
                CanBePastedIntoList(tagTypes.toList()[0])
            } else if (attribute == TagAttribute.VALUE && tagTypes.size == 1) {
                CanBePastedIntoArray(tagTypes.toList()[0])
            } else {
                CannotBePasted
            }
        }

        fun pasteEnabled(content: Pair<PasteTarget, List<ActualDoodle>>? = stack.lastOrNull()): Boolean {
            if (content == null) return false

            if (ui.selected.isEmpty()) return false
            if (ui.selected.size > 1) return false

            val selected = ui.selected[0]
            if (selected !is NbtDoodle) return false

            val (target, items) = content

            return when (target) {
                CannotBePasted -> false
                CanBePastedIntoCompound -> {
                    if (!selected.tag.type.isCompound()) false
                    else {
                        val tag = selected.tag.getAs<CompoundTag>().value.find { tag ->
                            items.find { it is NbtDoodle && it.name == tag.name } != null
                        }
                        tag == null
                    }
                }
                is CanBePastedIntoList -> {
                    selected.tag.type == TagType.TAG_LIST && selected.tag.getAs<ListTag>().let { it.elementsType == target.elementsType || it.elementsType == TagType.TAG_END }
                }
                is CanBePastedIntoArray -> selected.tag.type == target.arrayTagType
            }
        }

        fun yank() {
            val target = pasteTarget
            if (target == CannotBePasted) return

            if (stack.size >= 5) stack.removeAt(0)
            stack.add(
                Pair(target, mutableListOf<ActualDoodle>().apply { addAll(ui.selected.map { it.clone(null) }) })
            )
        }

        fun paste() {
            if (ui.selected.isEmpty()) throw NoSelectedItemsException("paste")
            if (ui.selected.size > 1) throw TooManyItemsSelectedException("paste")

            val selected = ui.selected[0]
            if (selected !is NbtDoodle)
                throw InternalAssertionException(NbtDoodle::class.java.simpleName, selected.javaClass.name)

            val (target, doodles) = stack.last()

            when (target) {
                CannotBePasted -> throw InternalAssertionException(
                    listOf(CanBePastedIntoCompound::class.java.simpleName, CanBePastedIntoArray::class.java.simpleName, CanBePastedIntoList::class.java.simpleName),
                    CannotBePasted::class.java.simpleName
                )
                CanBePastedIntoCompound -> {
                    if (selected.tag.type != TagType.TAG_COMPOUND)
                        throw InvalidPasteTargetException(selected.tag.type)
                }
                is CanBePastedIntoList -> {
                    if (selected.tag.type != TagType.TAG_LIST)
                        throw InvalidPasteTargetException(selected.tag.type)

                    val listTag = selected.tag.getAs<ListTag>()
                    if (target.elementsType != listTag.elementsType && listTag.elementsType != TagType.TAG_END)
                        throw PasteTargetTypeMismatchException(listTag.elementsType, target.elementsType)
                }
                is CanBePastedIntoArray -> {
                    if (selected.tag.type != target.arrayTagType)
                        throw InvalidValuePasteTargetException(target.arrayTagType)
                }
            }

            val created = doodles.map { selected.create(it.clone(selected), false) }

            selected.update(NbtDoodle.UpdateTarget.VALUE)
            selected.expand()

            ui.selected.clear()
            ui.selected.addAll(created)

            actions.history.newAction(PasteDoodleAction(created))
        }

    }

    inner class CreateAction {

        val internal = Internal()

        fun prepare(type: TagType) {
            if (ui.selected.size > 1) throw TooManyItemsSelectedException("create")

            val into = ui.selected.firstOrNull() ?: rootDoodle
            if (into !is NbtDoodle) throw InternalAssertionException(NbtDoodle::class.java.simpleName, into.javaClass.name)

            into.expand()

            if (into.tag.type.isArray()) {
                into.creator = ValueCreationDoodle(
                    into.depth + 1, 0, into, VirtualDoodle.VirtualMode.CREATE
                )
            } else {
                into.creator = NbtCreationDoodle(
                    type, into.depth + 1, 0, into, VirtualDoodle.VirtualMode.CREATE
                )
            }
        }

        fun cancel() {
            if (ui.selected.size > 1) throw VirtualActionCancelException("creation")

            val into = ui.selected.firstOrNull() ?: rootDoodle
            if (into !is NbtDoodle) throw InternalAssertionException(NbtDoodle::class.java.simpleName, into.javaClass.name)

            into.creator = null
        }

        fun create(new: ActualDoodle, into: NbtDoodle) {
            val (conflict, index) = new.checkNameConflict()
            if (conflict) throw NameConflictException("create", (new as NbtDoodle).name!!, index)

            new.parent = into

            if (!into.expanded) into.expand()

            into.create(new)
            into.update(NbtDoodle.UpdateTarget.VALUE, NbtDoodle.UpdateTarget.INDEX)

            into.creator = null

            ui.selected.clear()
            ui.selected.add(new)

            actions.history.newAction(CreateDoodleAction(new))
        }

        inner class Internal {

            fun create(targets: List<ActualDoodle>) {
                targets.forEach {
                    val eachParent = it.parent ?: throw ParentNotFoundException()

                    if (!eachParent.expanded)
                        eachParent.expand()

                    eachParent.create(it)
                }
                targets.map { it.parent }.toSet()
                    .forEach { it?.update(NbtDoodle.UpdateTarget.VALUE, NbtDoodle.UpdateTarget.INDEX) }
                ui.selected.clear()
                ui.selected.addAll(targets)
            }

        }

    }

    inner class EditAction {

        val internal = Internal()

        fun prepare() {
            if (ui.selected.isEmpty()) throw NoSelectedItemsException("edit")
            if (ui.selected.size > 1) throw AttemptToEditMultipleTagsException()

            when (val target = ui.selected[0]) {
                is NbtDoodle -> target.parent?.creator = NbtCreationDoodle(target, VirtualDoodle.VirtualMode.EDIT)
                is ValueDoodle ->
                    target.parent?.creator = ValueCreationDoodle(target, VirtualDoodle.VirtualMode.EDIT)
            }
        }

        fun cancel() {
            if (ui.selected.isEmpty() || ui.selected.size > 1) throw VirtualActionCancelException("edition")

            val targetParent = ui.selected[0].parent ?: throw ParentNotFoundException()
            targetParent.creator = null
        }

        fun edit(oldActual: ActualDoodle, newActual: ActualDoodle) {
            internal.edit(oldActual, newActual)

            actions.history.newAction(EditDoodleAction(oldActual, newActual))
        }

        inner class Internal {

            fun edit(oldActual: ActualDoodle, newActual: ActualDoodle) {
                val into = oldActual.parent ?: throw ParentNotFoundException()

                val (conflict, index) = newActual.checkNameConflict()
                if (conflict) throw NameConflictException("edit", (newActual as NbtDoodle).name!!, index)

                newActual.parent = into

                actions.deleter.internal.delete(listOf(oldActual))
                actions.creator.internal.create(listOf(newActual))

                into.creator = null

                ui.selected.clear()
                ui.selected.add(newActual)
            }

        }

    }

    inner class DeleteAction {

        val internal = Internal()

        fun delete() {
            ui.selected.sortBy { doodles.cached.indexOf(it) }

            val deleted = ui.selected.mapNotNull { it.delete() }
            deleted.forEach { it.parent?.update(NbtDoodle.UpdateTarget.VALUE, NbtDoodle.UpdateTarget.INDEX) }

            ui.selected.clear()

            actions.history.newAction(DeleteDoodleAction(deleted))
        }

        inner class Internal {

            fun delete(targets: List<ActualDoodle>) {
                targets.forEach {
                    it.delete()
                }
                targets.map { it.parent }.toSet()
                    .forEach { it?.update(NbtDoodle.UpdateTarget.VALUE, NbtDoodle.UpdateTarget.INDEX) }
                ui.selected.clear()
            }

        }

    }

    private enum class TagAttribute {
        NAMED, UNNAMED, VALUE
    }

}
