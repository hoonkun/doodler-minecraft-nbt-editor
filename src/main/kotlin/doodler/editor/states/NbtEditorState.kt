package doodler.editor.states

import doodler.extension.removeRange
import doodler.extension.toRanges
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
import doodler.doodle.structures.Doodle
import doodler.editor.structures.*
import doodler.exceptions.*
import doodler.file.StateFile
import java.io.File


private val ClipboardStack = mutableStateListOf<Pair<PasteCriteria, List<ReadonlyDoodle>>>()

@Stable
class NbtEditorState(
    val root: TagDoodle,
    val file: StateFile,
    private val type: WorldFileType
): EditorState() {

    val logs = mutableStateListOf<EditorLog>()
    val currentLog = mutableStateOf<EditorLog?>(null)

    val selected = mutableStateListOf<ReadonlyDoodle>()
    var focused by mutableStateOf<ReadonlyDoodle?>(null)

    val lazyState = LazyListState()

    val items: SnapshotStateList<Doodle> by derivedStateOf { root.items }
    val action by derivedStateOf { items.find { it is ActionDoodle } as? ActionDoodle }

    var lastSaveUid by mutableStateOf(0L)

    private val actions = NbtEditorActions()
    val actionFlags = NbtEditorActionFlags()

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

    fun writeLog(log: EditorLog) {
        if (logs.size > 10) logs.removeFirst()
        logs.add(log)
        currentLog.value = log
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
    inner class NbtEditorActionFlags {

        val canBeSaved by derivedStateOf { actions.history.lastActionUid != lastSaveUid }
        val canBeUndo by derivedStateOf { actions.history.canBeUndo }
        val canBeRedo by derivedStateOf { actions.history.canBeRedo }

        val canBeCopied by derivedStateOf { actions.clipboard.copyEnabled }
        val canBePasted by derivedStateOf { actions.clipboard.pasteEnabled }

        val canBeEdited by derivedStateOf { actions.editor.canBeEdited }

        val pasteCriteria by derivedStateOf { actions.clipboard.criteria }
        val isClipboardEmpty by derivedStateOf { actions.clipboard.isStackEmpty }

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

        private val internal = Internal()

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

        val isStackEmpty by derivedStateOf { ClipboardStack.isEmpty() }

        val criteria by derivedStateOf {
            val targets = selected
            val attributes = setOf(
                *targets.map {
                    if (it is ArrayValueDoodle) TagAttribute.Value
                    else if (it is TagDoodle && it.tag.name != null) TagAttribute.Named
                    else TagAttribute.Unnamed
                }.toTypedArray()
            )
            val tagTypes = setOf(
                *targets.mapNotNull {
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

            criteria
        }

        val copyEnabled by derivedStateOf { criteria != CannotBePasted }

        val pasteEnabled by derivedStateOf {
            if (selected.isEmpty()) return@derivedStateOf false
            if (selected.size > 1) return@derivedStateOf false

            val into = selected[0]
            if (into !is TagDoodle) return@derivedStateOf false

            val (criteria, items) = ClipboardStack.lastOrNull() ?: return@derivedStateOf false

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
            if (criteria == CannotBePasted) return

            if (ClipboardStack.size >= 5) ClipboardStack.removeFirst()

            ClipboardStack.add(Pair(criteria, listOf(*selected.map { it.clone(null) }.toTypedArray())))
        }

        fun paste() {
            if (selected.isEmpty()) throw NoSelectedItemsException("paste")
            if (selected.size > 1) throw TooManyItemsSelectedException("paste")

            val into = selected[0]
            if (into !is TagDoodle)
                throw InternalAssertionException(TagDoodle::class.java.simpleName, selected.javaClass.name)

            val (criteria, items) = ClipboardStack.last()

            when (criteria) {
                CannotBePasted -> throw InternalAssertionException(
                    listOf(
                        CanBePastedIntoCompound::class.java.simpleName,
                        CanBePastedIntoArray::class.java.simpleName,
                        CanBePastedIntoList::class.java.simpleName
                    ),
                    CannotBePasted::class.java.simpleName
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

            actions.history.newAction(PasteActionSnapshot(uid(), created.snapshot()))

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
                ?: throw InternalAssertionException(TagDoodle::class.java.simpleName, "null")

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

        fun create(new: ReadonlyDoodle, where: Int? = null) {
            val into = createInto ?: throw VirtualActionCancelException("creation")

            val conflict = (new as? TagDoodle)?.hasConflict()
            if (conflict != null) throw NameConflictException("create", conflict.name ?: "root", conflict.where)

            if (!into.expanded) into.expand()

            into.createChild(new, where)
            into.action = null

            new.parent = into

            actions.selector.select(new)
            actions.history.newAction(CreateActionSnapshot(uid(), new.snapshot()))

            createInto = null
        }

        inner class Internal {

            fun create(targets: List<ReadonlyDoodleSnapshot>) {
                targets.forEach {
                    val eachParent = it.doodle.parent ?: throw ParentNotFoundException()
                    if (!eachParent.expanded) eachParent.expand()

                    eachParent.createChild(it.doodle, it.index)
                }

                actions.selector.select(targets.map { it.doodle })
            }

            fun create(target: ReadonlyDoodleSnapshot) {
                val eachParent = target.doodle.parent ?: throw ParentNotFoundException()
                if (!eachParent.expanded) eachParent.expand()

                eachParent.createChild(target.doodle, target.index)

                actions.selector.select(target.doodle)
            }

        }

    }

    @Stable
    inner class Editor: Action() {

        private val internal = Internal()

        private var editTarget: ReadonlyDoodle? = null

        val canBeEdited by derivedStateOf {
            if (selected.size > 1) return@derivedStateOf false
            val first = selected.firstOrNull() ?: return@derivedStateOf false
            if (first !is TagDoodle) return@derivedStateOf true

            first.tag.name != null
        }

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
            val oldSnapshot = old.snapshot()
            val newSnapshot = ReadonlyDoodleSnapshot(new, oldSnapshot.index)
            internal.edit(oldSnapshot, newSnapshot)
            actions.history.newAction(EditActionSnapshot(uid(), oldSnapshot, newSnapshot))
        }

        inner class Internal {

            fun edit(old: ReadonlyDoodleSnapshot, new: ReadonlyDoodleSnapshot) {
                val into = old.doodle.parent ?: throw ParentNotFoundException()

                if (old.doodle is TagDoodle && new.doodle is TagDoodle && old.doodle.tag.name != new.doodle.tag.name) {
                    val conflict = (new.doodle as? TagDoodle)?.hasConflict()
                    if (conflict != null) throw NameConflictException("edit", conflict.name ?: "root", conflict.where)
                }

                actions.deleter.internal.delete(old)
                actions.creator.internal.create(new)

                new.doodle.parent = into

                into.action = null

                actions.selector.select(new.doodle)
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
                    selected.sortedBy { items.indexOf(it) }
                        .map { it.snapshot() }
                        .onEach { it.doodle.delete() }
                        .toMutableStateList()
                )
            )
            selected.clear()
        }

        inner class Internal {

            fun delete(targets: List<ReadonlyDoodleSnapshot>) {
                targets.forEach { it.doodle.delete() }
                selected.clear()
            }

            fun delete(target: ReadonlyDoodleSnapshot) {
                target.doodle.delete()
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

        fun multiSelectRange(target: ReadonlyDoodle) {
            val lastSelected = selected.lastOrNull() ?: return select(target)
            val from = items.indexOf(lastSelected)
            val to = items.indexOf(target)

            val targets = items.filterIsInstance<ReadonlyDoodle>()
                .slice(
                    if (from < to) from + 1 until to + 1
                    else to until from
                )
                .filter { !selected.contains(it) }

            selected.addAll(targets)
        }

        fun multiSelectSingle(target: ReadonlyDoodle) {
            if (!selected.contains(target)) selected.add(target)
        }

        fun unselect(target: ReadonlyDoodle) {
            if (selected.contains(target)) selected.remove(target)
        }

    }

}