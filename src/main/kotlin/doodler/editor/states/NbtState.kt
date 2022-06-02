package doodler.editor.states

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import doodler.doodle.*
import doodler.doodle.structures.*
import doodler.extensions.removeRange
import doodler.extensions.toRanges
import doodler.files.StateFile
import doodler.minecraft.DatWorker
import doodler.minecraft.McaWorker
import doodler.minecraft.structures.DatFileType
import doodler.minecraft.structures.McaFileType
import doodler.minecraft.structures.WorldFileType
import doodler.nbt.TagType
import doodler.nbt.tag.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

@Stable
class NbtState (
    val rootDoodle: NbtDoodle,
    private val file: StateFile,
    private val fileType: WorldFileType
) {

    companion object {
        fun new(
            rootTag: CompoundTag,
            file: File,
            fileType: WorldFileType
        ) = NbtState(
            NbtDoodle(rootTag, -1),
            StateFile(file.name, file.absolutePath, file.isDirectory, file.isFile),
            fileType
        )
    }

    private val logs = mutableStateListOf<DoodleLog>()

    val currentLogState: MutableState<DoodleLog?> = mutableStateOf(null)
    private var currentLog by currentLogState

    val ui by mutableStateOf(DoodleUi())
    val lazyState = LazyListState()

    val doodles get() = rootDoodle.doodles
    val virtual by derivedStateOf { doodles.find { it is VirtualDoodle } as? VirtualDoodle? }

    val actions = Actions()

    var lastSaveUid by mutableStateOf(0L)

    private val virtualScrollTo by derivedStateOf {
        virtual.let { if (it == null) null else checkVirtualIsInvisible(it) }
    }

    init {
        rootDoodle.expand()

        val hasOnlyExpandableChild = rootDoodle.children
            .let { children -> children.size == 1 && children[0].let { it is NbtDoodle && it.tag.canHaveChildren } }

        if (hasOnlyExpandableChild) (rootDoodle.children[0] as NbtDoodle).expand()
    }

    fun save() {
        when (fileType) {
            DatFileType -> DatWorker.write(rootDoodle.tag.getAs(), File(file.absolutePath))
            is McaFileType -> McaWorker.writeChunk(File(file.absolutePath), rootDoodle.tag.getAs(), fileType.location)
        }

        lastSaveUid = actions.history.lastActionUid

        newLog(
            DoodleLog(
                DoodleLogLevel.SUCCESS,
                "Success!",
                "File Saved",
                "Successfully saved nbt into file '${file.name}'"
            )
        )
    }

    private fun checkVirtualIsInvisible(newVirtual: VirtualDoodle): Int? {
        val index = doodles.indexOf(newVirtual)
        val firstIndex = lazyState.firstVisibleItemIndex
        val windowSize = lazyState.layoutInfo.visibleItemsInfo.size
        val invisible = index < firstIndex || index >= firstIndex + windowSize

        return if (invisible) (index - windowSize / 2).coerceAtLeast(0) else null
    }

    fun scrollToVirtual(scope: CoroutineScope) {
        virtualScrollTo.let {
            if (it == null) return
            scope.launch { lazyState.scrollToItem(it) }
        }
    }

    fun newLog(new: DoodleLog) {
        if (logs.size > 10) logs.removeFirst()
        logs.add(new)
        currentLog = new
    }

    @Stable
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
                newLog(DoodleLog(DoodleLogLevel.FATAL, exception.title, exception.summary, exception.description))
                exception.printStackTrace()
            }
            catch (exception: Exception) {
                exception.printStackTrace()
            }
        }

    }

    @Stable
    open inner class Action {
        fun uid(): Long = System.currentTimeMillis()
    }

    @Stable
    inner class HistoryAction: Action() {

        private val undoStack: SnapshotStateList<DoodleAction> = mutableStateListOf()
        private val redoStack: SnapshotStateList<DoodleAction> = mutableStateListOf()

        val canBeUndo: Boolean by derivedStateOf { undoStack.isNotEmpty() }
        val canBeRedo: Boolean by derivedStateOf { redoStack.isNotEmpty() }

        var lastActionUid by mutableStateOf(0L)

        fun newAction(action: DoodleAction) {
            lastActionUid = action.uid
            redoStack.clear()
            undoStack.add(action)
            updateLastAction()
        }

        fun undo() {
            if (!canBeUndo) return

            val action = undoStack.removeLast()
            redoStack.add(action)
            updateLastAction()

            when (action) {
                is DeleteDoodleAction -> undoDelete(action)
                is PasteDoodleAction -> undoPaste(action)
                is CreateDoodleAction -> undoCreate(action)
                is EditDoodleAction -> undoEdit(action)
                is MoveDoodleAction -> undoMove(action)
            }
        }

        fun redo() {
            if (!canBeRedo) return

            val action = redoStack.removeLast()

            undoStack.add(action)
            updateLastAction()

            when (action) {
                is DeleteDoodleAction -> redoDelete(action)
                is PasteDoodleAction -> redoPaste(action)
                is CreateDoodleAction -> redoCreate(action)
                is EditDoodleAction -> redoEdit(action)
                is MoveDoodleAction -> redoMove(action)
            }
        }

        private fun updateLastAction() {
            lastActionUid = undoStack.lastOrNull()?.uid ?: 0L
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

    @Stable
    inner class MoveAction: Action() {

        val internal = Internal()

        fun moveUp(targets: SnapshotStateList<ActualDoodle>) {
            internal.moveUp(targets)

            actions.history.newAction(MoveDoodleAction(uid(), MoveDoodleAction.DoodleMoveDirection.UP, targets))
        }

        fun moveDown(targets: SnapshotStateList<ActualDoodle>) {
            internal.moveDown(targets)

            actions.history.newAction(MoveDoodleAction(uid(), MoveDoodleAction.DoodleMoveDirection.DOWN, targets))
        }

        @Stable
        inner class Internal {

            fun moveUp(targets: List<ActualDoodle>) {
                move(targets) { it.first - 1 }
            }

            fun moveDown(targets: List<ActualDoodle>) {
                move(targets) { it.first + 1 }
            }

            private fun move(targets: List<ActualDoodle>, into: (IntRange) -> Int) {
                val targetsRange = targets.map { it.index() }.toRanges()
                if (targetsRange.size > 1)
                    throw InvalidOperationException("Cannot move", "Selection range is invalid. Only continuous range is available for this action.")
                if (targetsRange.isEmpty())
                    throw InvalidOperationException("Cannot move", "No targets selected.")

                val range = targetsRange[0]

                val variants = targets.map { if (it is NbtDoodle) "nbt" else "value" }.toSet().toList()
                if (variants.size > 1)
                    throw InternalAssertionException(listOf(NbtDoodle::class.java.simpleName, ValueDoodle::class.java.simpleName), "Both")

                val parent = targets.first().parent ?: throw ParentNotFoundException()
                val parentTag = parent.tag

                parent.children.addAll(into(range), parent.children.removeRange(range))

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
            }

        }

    }

    @Stable
    inner class ClipboardAction: Action() {

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
                            items.find { it is NbtDoodle && it.tag.name == tag.name } != null
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

            val created = doodles.map { selected.create(it.cloneAsChild(selected)) }.toMutableStateList()

            selected.expand()

            ui.selected.clear()
            ui.selected.addAll(created)

            actions.history.newAction(PasteDoodleAction(uid(), created))
        }

    }

    @Stable
    inner class CreateAction: Action() {

        val internal = Internal()

        fun prepare(type: TagType) {
            if (ui.selected.size > 1) throw TooManyItemsSelectedException("create")

            val into = ui.selected.firstOrNull() ?: rootDoodle
            if (into !is NbtDoodle) throw InternalAssertionException(NbtDoodle::class.java.simpleName, into.javaClass.name)

            into.expand()

            val newVirtual =
                if (into.tag.type.isArray()) {
                    ValueCreationDoodle(into.depth + 1, into)
                } else {
                    NbtCreationDoodle(type, into.depth + 1, into)
                }

            into.virtual = newVirtual
        }

        fun cancel() {
            if (ui.selected.size > 1) throw VirtualActionCancelException("creation")

            val into = ui.selected.firstOrNull() ?: rootDoodle
            if (into !is NbtDoodle) throw InternalAssertionException(NbtDoodle::class.java.simpleName, into.javaClass.name)

            into.virtual = null
        }

        fun create(new: ActualDoodle, into: NbtDoodle, where: Int) {
            val (conflict, index) = new.checkNameConflict()
            if (conflict) throw NameConflictException("create", (new as NbtDoodle).tag.name!!, index)

            new.parent = into

            if (!into.expanded) into.expand()

            into.create(new, where)

            into.virtual = null

            ui.selected.clear()
            ui.selected.add(new)

            actions.history.newAction(CreateDoodleAction(uid(), new))
        }

        @Stable
        inner class Internal {

            fun create(targets: List<ActualDoodle>) {
                targets.forEach {
                    val eachParent = it.parent ?: throw ParentNotFoundException()

                    if (!eachParent.expanded)
                        eachParent.expand()

                    eachParent.create(it, it.index())
                }
                ui.selected.clear()
                ui.selected.addAll(targets)
            }

        }

    }

    @Stable
    inner class EditAction: Action() {

        val internal = Internal()

        fun prepare() {
            if (ui.selected.isEmpty()) throw NoSelectedItemsException("edit")
            if (ui.selected.size > 1) throw AttemptToEditMultipleTagsException()

            val target = ui.selected[0]
            val newVirtual = when (target) {
                is NbtDoodle -> NbtEditionDoodle(target)
                is ValueDoodle -> ValueEditionDoodle(target)
            }

            target.parent?.virtual = newVirtual
        }

        fun cancel() {
            if (ui.selected.isEmpty() || ui.selected.size > 1) throw VirtualActionCancelException("edition")

            val targetParent = ui.selected[0].parent ?: throw ParentNotFoundException()
            targetParent.virtual = null
        }

        fun edit(oldActual: ActualDoodle, newActual: ActualDoodle) {
            internal.edit(oldActual, newActual)

            actions.history.newAction(EditDoodleAction(uid(), oldActual, newActual))
        }

        @Stable
        inner class Internal {

            fun edit(oldActual: ActualDoodle, newActual: ActualDoodle) {
                val into = oldActual.parent ?: throw ParentNotFoundException()

                val (conflict, index) = newActual.checkNameConflict()
                if (conflict) throw NameConflictException("edit", (newActual as NbtDoodle).tag.name!!, index)

                newActual.parent = into

                actions.deleter.internal.delete(listOf(oldActual))
                actions.creator.internal.create(listOf(newActual))

                into.virtual = null

                ui.selected.clear()
                ui.selected.add(newActual)
            }

        }

    }

    @Stable
    inner class DeleteAction: Action() {

        val internal = Internal()

        fun delete() {
            ui.selected.sortBy { doodles.indexOf(it) }

            ui.selected.clear()

            actions.history.newAction(DeleteDoodleAction(uid(), ui.selected.mapNotNull { it.delete() }.toMutableStateList()))
        }

        @Stable
        inner class Internal {

            fun delete(targets: List<ActualDoodle>) {
                targets.forEach { it.delete() }
                ui.selected.clear()
            }

        }

    }

    private enum class TagAttribute {
        NAMED, UNNAMED, VALUE
    }

}

