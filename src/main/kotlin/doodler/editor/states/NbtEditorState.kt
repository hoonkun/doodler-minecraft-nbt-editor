package doodler.editor.states

import activator.doodler.doodle.*
import activator.doodler.extensions.removeRange
import activator.doodler.extensions.toRanges
import doodler.minecraft.DatWorker
import doodler.minecraft.McaWorker
import doodler.minecraft.structures.DatFileType
import doodler.minecraft.structures.McaFileType
import doodler.minecraft.structures.WorldFileType
import doodler.nbt.TagType
import doodler.nbt.tag.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import doodler.doodle.structures.*
import doodler.editor.structures.*
import doodler.exceptions.*
import doodler.file.StateFile
import java.io.File

class NbtEditorState(
    val root: TagDoodle,
    private val file: StateFile,
    private val type: WorldFileType
) {

     private val logs = mutableStateListOf<EditorLog>()
     val currentLog = mutableStateOf<EditorLog?>(null)

    val selected = mutableStateListOf<ReadonlyDoodle>()
    var focusedDepth by mutableStateOf<ReadonlyDoodle?>(null)

    val lazyState = LazyListState()

    val items get() = root.items
    val action by derivedStateOf { items.find { it is ActionDoodle } as? ActionDoodle }

    var lastSaveUid by mutableStateOf(0L)

    private val actions = NbtEditorActions()

    init {
        root.expand()

        val hasOnlyExpandableChild = root.children
            .let { children -> children.size == 1 && children[0].let { it is TagDoodle && it.tag.canHaveChildren } }

        if (hasOnlyExpandableChild) (root.children[0] as TagDoodle).expand()
    }

    fun save() {
        when (type) {
            DatFileType -> DatWorker.write(root.tag.getAs(), File(file.absolutePath))
            is McaFileType -> McaWorker.writeChunk(File(file.absolutePath), root.tag.getAs(), type.location)
        }

         lastSaveUid = actions.history.lastActionUid

         writeLog(
             level = EditorLogLevel.Success,
             title = "Success",
             summary = "File Saved",
             description = "Successfully saved nbt into file '${file.name}'"
         )
    }

    fun action(action: NbtEditorActions.() -> Unit) {
        try { actions.apply { action() } }
        catch(exception: DoodleException) {
            writeLog(EditorLogLevel.Fatal, exception.title, exception.summary, exception.description)
            exception.printStackTrace()
        }
        catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    suspend fun scrollToAction() {
        val action = action ?: return

        val index = items.indexOf(action)
        val firstIndex = lazyState.firstVisibleItemIndex
        val windowSize = lazyState.layoutInfo.visibleItemsInfo.size
        val invisible = index < firstIndex || index >= firstIndex + windowSize

        if (!invisible) return

        val scrollTo = (index - windowSize / 2).coerceAtLeast(0)
        lazyState.scrollToItem(scrollTo)
    }

    private fun writeLog(level: EditorLogLevel, title: String, summary: String?, description: String?) {
        val new = EditorLog(level, title, summary, description)
        if (logs.size > 10) logs.removeFirst()
        logs.add(new)
        currentLog.value = new
    }

    @Stable
    inner class NbtEditorActions {

        val history = History()

        val selector = Selector()

        val clipboard = Clipboard()
        
        val creator = Creator()

        val deleter = Deleter()
        
        val editor = Editor()
        
        val elevator = Elevator()

    }

    @Stable
    open inner class Action {
        fun uid(): Long = System.currentTimeMillis()
    }

    @Stable
    inner class History: Action() {

        private val undoStack: SnapshotStateList<NbtEditorActionSnapshot> = mutableStateListOf()
        private val redoStack: SnapshotStateList<NbtEditorActionSnapshot> = mutableStateListOf()

        val canBeUndo: Boolean by derivedStateOf { undoStack.isNotEmpty() }
        val canBeRedo: Boolean by derivedStateOf { redoStack.isNotEmpty() }

        val lastActionUid: Long by derivedStateOf { undoStack.lastOrNull()?.uid ?: 0L }

        fun newAction(snapshot: NbtEditorActionSnapshot) {
            redoStack.clear()
            undoStack.add(snapshot)
        }

        fun undo() {
            if (!canBeUndo) return
            
            val snapshot = undoStack.removeLast()
            redoStack.add(snapshot)
            
            when (snapshot) {
                is DeleteActionSnapshot -> actions.deleter.undo(snapshot)
                is PasteActionSnapshot -> actions.clipboard.undo(snapshot)
                is CreateActionSnapshot -> actions.creator.undo(snapshot)
                is EditActionSnapshot -> actions.editor.undo(snapshot)
                is MoveActionSnapshot -> actions.elevator.undo(snapshot)
            }
        }

        fun redo() {
            if (!canBeRedo) return
            
            val snapshot = redoStack.removeLast()
            undoStack.add(snapshot)
            
            when (snapshot) {
                is DeleteActionSnapshot -> actions.deleter.redo(snapshot)
                is PasteActionSnapshot -> actions.clipboard.redo(snapshot)
                is CreateActionSnapshot -> actions.creator.redo(snapshot)
                is EditActionSnapshot -> actions.editor.redo(snapshot)
                is MoveActionSnapshot -> actions.elevator.redo(snapshot)
            }
        }

    }

    @Stable
    inner class Elevator: Action() {

        val internal = Internal()

        fun moveUp(targets: SnapshotStateList<ReadonlyDoodle>) {
            internal.moveUp(targets)

            actions.history.newAction(MoveActionSnapshot(uid(), MoveActionSnapshot.Direction.Up, targets))
        }

        fun moveDown(targets: SnapshotStateList<ReadonlyDoodle>) {
            internal.moveDown(targets)

            actions.history.newAction(MoveActionSnapshot(uid(), MoveActionSnapshot.Direction.Down, targets))
        }

        fun undo(snapshot: MoveActionSnapshot) {
            if (snapshot.direction == MoveActionSnapshot.Direction.Up)
                internal.moveDown(snapshot.moved)
            else if (snapshot.direction == MoveActionSnapshot.Direction.Down)
                internal.moveUp(snapshot.moved)
        }

        fun redo(snapshot: MoveActionSnapshot) {
            if (snapshot.direction == MoveActionSnapshot.Direction.Up)
                internal.moveUp(snapshot.moved)
            else if (snapshot.direction == MoveActionSnapshot.Direction.Down)
                internal.moveDown(snapshot.moved)
        }

        @Stable
        inner class Internal {

            fun moveUp(targets: List<ReadonlyDoodle>) {
                move(targets) { it.first - 1 }
            }

            fun moveDown(targets: List<ReadonlyDoodle>) {
                move(targets) { it.first + 1 }
            }

            private fun move(targets: List<ReadonlyDoodle>, into: (IntRange) -> Int) {
                val targetsRange = targets.map { it.index }.toRanges()
                if (targetsRange.size > 1)
                    throw InvalidOperationException(
                        "Cannot move",
                        "Selection range is invalid. Only continuous range is available for this action."
                    )
                if (targetsRange.isEmpty())
                    throw InvalidOperationException("Cannot move", "No targets selected.")

                val range = targetsRange[0]

                val variants = targets.map { it.javaClass.simpleName }.toSet()
                if (variants.size > 1)
                    throw InternalAssertionException(
                        expected = listOf(TagDoodle::class, ArrayValueDoodle::class).map { it.java.simpleName },
                        actual = "Both"
                    )

                val parent = targets[0].parent ?: throw ParentNotFoundException()
                val parentTag = parent.tag

                val ins = into(range)
                parent.children.addAll(ins, parent.children.removeRange(range))

                when (parentTag) {
                    is CompoundTag -> parentTag.getAs<CompoundTag>()
                        .apply { value.addAll(ins, value.removeRange(range)) }
                    is ListTag -> parentTag.getAs<ListTag>()
                        .apply { value.addAll(ins, value.removeRange(range)) }
                    is ByteArrayTag -> parentTag.getAs<ByteArrayTag>()
                        .apply { value = value.toMutableList().apply { addAll(ins, removeRange(range)) }.toByteArray() }
                    is IntArrayTag -> parentTag.getAs<IntArrayTag>()
                        .apply { value = value.toMutableList().apply { addAll(ins, removeRange(range)) }.toIntArray() }
                    is LongArrayTag -> parentTag.getAs<LongArrayTag>()
                        .apply { value = value.toMutableList().apply { addAll(ins, removeRange(range)) }.toLongArray() }
                }
            }

        }

    }

    @Stable
    inner class Clipboard: Action() {

        val stack = mutableStateListOf<Pair<PasteCriteria, List<ReadonlyDoodle>>>()

        val pasteEnabled by derivedStateOf {
            if (selected.isEmpty()) return@derivedStateOf false
            if (selected.size > 1) return@derivedStateOf false

            val into = selected[0]
            if (into !is TagDoodle) return@derivedStateOf false

            val (criteria, items) = stack.lastOrNull() ?: return@derivedStateOf false

            when (criteria) {
                CannotBePasted -> false
                CanBePastedIntoCompound -> {
                    if (!into.tag.type.isCompound()) false
                    else !into.tag.getAs<CompoundTag>().value.any { tag ->
                        items.any { it is TagDoodle && it.tag.name == tag.name }
                    }
                }
                is CanBePastedIntoList -> {
                    val isList = into.tag.type == TagType.TAG_LIST
                    val listTypeMatch = into.tag.getAs<ListTag>()
                        .let { it.elementsType == criteria.elementsType || it.elementsType == TagType.TAG_END }
                    isList && listTypeMatch
                }
                is CanBePastedIntoArray -> into.tag.type == criteria.arrayTagType
            }
        }

        fun undo(snapshot: PasteActionSnapshot) {
            actions.deleter.internal.delete(snapshot.created)
        }

        fun redo(snapshot: PasteActionSnapshot) {
            actions.creator.internal.create(snapshot.created)
        }

        fun copy() {
            val targets = selected
            val attributes = setOf(*
                targets.map {
                    if (it is ArrayValueDoodle) TagAttribute.Value
                    else if (it is TagDoodle && it.tag.name != null) TagAttribute.Named
                    else TagAttribute.Unnamed
                }.toTypedArray()
            )
            val tagTypes = setOf(*
                targets.mapNotNull {
                    when (it) {
                        is TagDoodle -> it.tag.type
                        is ArrayValueDoodle -> it.parent?.tag?.type
                    }
                }.toTypedArray()
            )

            val criteria =
                if (attributes.size != 1) CannotBePasted
                else {
                    val attribute = attributes.iterator().next()
                    if (attribute == TagAttribute.Named) CanBePastedIntoCompound
                    else if (attribute == TagAttribute.Unnamed && tagTypes.size == 1)
                        CanBePastedIntoList(tagTypes.iterator().next())
                    else if (attribute == TagAttribute.Value && tagTypes.size == 1)
                        CanBePastedIntoArray(tagTypes.iterator().next())
                    else CannotBePasted
                }

            if (criteria == CannotBePasted) return

            if (stack.size >= 5) stack.removeFirst()

            stack.add(Pair(criteria, listOf(*targets.map { it.clone(null) }.toTypedArray())))
        }

        fun paste() {
            if (selected.isEmpty()) throw NoSelectedItemsException("paste")
            if (selected.size > 1) throw TooManyItemsSelectedException("paste")

            val into = selected[0]
            if (into !is TagDoodle)
                throw InternalAssertionException(TagDoodle::class.java.simpleName, selected.javaClass.name)

            val (criteria, items) = stack.last()

            when (criteria) {
                CannotBePasted -> throw InternalAssertionException(
                    listOf(activator.doodler.doodle.structures.CanBePastedIntoCompound::class.java.simpleName, activator.doodler.doodle.structures.CanBePastedIntoArray::class.java.simpleName, activator.doodler.doodle.structures.CanBePastedIntoList::class.java.simpleName),
                    activator.doodler.doodle.structures.CannotBePasted::class.java.simpleName
                )
                CanBePastedIntoCompound ->
                    if (!into.tag.type.isCompound()) throw InvalidPasteTargetException(into.tag.type)
                is CanBePastedIntoList -> {
                    if (!into.tag.type.isList()) throw InvalidPasteTargetException(into.tag.type)

                    val tag = into.tag.getAs<ListTag>()
                    if (criteria.elementsType != tag.elementsType && !tag.elementsType.isEnd())
                        throw InvalidPasteTargetException(into.tag.type)
                }
                is CanBePastedIntoArray ->
                    if (into.tag.type != criteria.arrayTagType) throw InvalidPasteTargetException(into.tag.type)
            }

            val created = items.map { into.createChild(it.clone(into, it.depth + 1)) }.toMutableStateList()

            into.expand()

            actions.selector.select(created)

            actions.history.newAction(PasteActionSnapshot(uid(), created))

        }

    }

    @Stable
    inner class Creator: Action() {

        val internal = Internal()

        private var createInto: TagDoodle? = null

        fun undo(snapshot: CreateActionSnapshot) {
            actions.deleter.internal.delete(snapshot.created)
        }

        fun redo(snapshot: CreateActionSnapshot) {
            internal.create(snapshot.created)
        }

        fun prepare(type: TagType) {
            if (selected.size > 1) throw TooManyItemsSelectedException("create")

            val into = (selected.firstOrNull() ?: root) as? TagDoodle
                ?: throw InternalAssertionException(NbtDoodle::class.java.simpleName, "null")

            into.expand()

            into.action =
                if (into.tag.type.isArray()) ArrayValueCreatorDoodle(into)
                else TagCreatorDoodle(type, into)

            createInto = into
        }

        fun cancel() {
            val into = createInto ?: throw VirtualActionCancelException("creation")

            into.action = null

            createInto = null
        }

        fun create(new: ReadonlyDoodle, where: Int) {
            val into = createInto ?: throw VirtualActionCancelException("creation")

            val conflict = (new as? TagDoodle)?.hasConflict()
            if (conflict != null) throw NameConflictException("create", conflict.name ?: "root", conflict.where)

            if (!into.expanded) into.expand()

            into.createChild(new, where)
            into.action = null

            new.parent = into

            actions.selector.select(new)
            actions.history.newAction(CreateActionSnapshot(uid(), new))

            createInto = null
        }

        inner class Internal {

            fun create(targets: List<ReadonlyDoodle>) {
                targets.forEach {
                    val eachParent = it.parent ?: throw ParentNotFoundException()
                    if (!eachParent.expanded) eachParent.expand()

                    eachParent.createChild(it, it.index)
                }

                actions.selector.select(targets)
            }

            fun create(target: ReadonlyDoodle) {
                val eachParent = target.parent ?: throw ParentNotFoundException()
                if (!eachParent.expanded) eachParent.expand()

                eachParent.createChild(target, target.index)

                actions.selector.select(target)
            }

        }

    }

    @Stable
    inner class Editor: Action() {

        private val internal = Internal()

        private var editTarget: ReadonlyDoodle? = null

        fun undo(snapshot: EditActionSnapshot) {
            internal.edit(snapshot.new, snapshot.old)
        }

        fun redo(snapshot: EditActionSnapshot) {
            internal.edit(snapshot.old, snapshot.new)
        }

        fun prepare() {
            if (selected.isEmpty()) throw NoSelectedItemsException("edit")
            if (selected.size > 1) throw AttemptToEditMultipleTagsException()

            val target = selected[0]
            target.parent?.action =
                when (target) {
                    is TagDoodle -> TagEditorDoodle(target)
                    is ArrayValueDoodle -> ArrayValueEditorDoodle(target)
                }

            editTarget = target
        }

        fun cancel() {
            val editTarget = editTarget ?: throw VirtualActionCancelException("edition")
            val parent = editTarget.parent ?: throw ParentNotFoundException()

            parent.action = null
        }

        fun edit(old: ReadonlyDoodle, new: ReadonlyDoodle) {
            internal.edit(old, new)
            actions.history.newAction(EditActionSnapshot(uid(), old, new))
        }

        inner class Internal {

            fun edit(old: ReadonlyDoodle, new: ReadonlyDoodle) {
                val into = old.parent ?: throw ParentNotFoundException()

                val conflict = (new as? TagDoodle)?.hasConflict()
                if (conflict != null) throw NameConflictException("edit", conflict.name ?: "root", conflict.where)

                actions.deleter.internal.delete(old)
                actions.creator.internal.create(new)

                new.parent = into

                into.action = null

                actions.selector.select(new)
            }

        }

    }

    @Stable
    inner class Deleter: Action() {

        val internal = Internal()

        fun undo(snapshot: DeleteActionSnapshot) {
            actions.creator.internal.create(snapshot.deleted)
        }

        fun redo(snapshot: DeleteActionSnapshot) {
            internal.delete(snapshot.deleted)
        }

        fun delete() {
            actions.history.newAction(
                DeleteActionSnapshot(
                    uid(),
                    selected.sortedBy { items.indexOf(it) }.onEach { it.delete() }.toMutableStateList()
                )
            )
            selected.clear()
        }

        inner class Internal {

            fun delete(targets: List<ReadonlyDoodle>) {
                targets.forEach { it.delete() }
                selected.clear()
            }

            fun delete(target: ReadonlyDoodle) {
                target.delete()
                selected.clear()
            }

        }

    }

    inner class Selector {

        fun select(target: ReadonlyDoodle) {
            selected.clear()
            selected.add(target)
        }

        fun select(targets: List<ReadonlyDoodle>) {
            selected.clear()
            selected.addAll(targets)
        }

        fun multiSelectRange(items: List<ReadonlyDoodle>) {
            selected.addAll(items.filter { !selected.contains(it) })
        }

        fun multiSelectSingle(target: ReadonlyDoodle) {
            if (!selected.contains(target)) selected.add(target)
        }

        fun unselect(target: ReadonlyDoodle) {
            if (selected.contains(target)) selected.remove(target)
        }

    }

}