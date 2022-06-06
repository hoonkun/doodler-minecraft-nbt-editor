package doodler.editor.structures

import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshots.SnapshotStateList
import activator.doodler.nbt.TagType
import doodler.doodle.structures.ReadonlyDoodle

@Stable
abstract class NbtEditorActionSnapshot(
    val uid: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NbtEditorActionSnapshot

        return uid == other.uid
    }

    override fun hashCode(): Int = uid.hashCode()
}

@Stable
class DeleteActionSnapshot(
    uid: Long,
    val deleted: SnapshotStateList<ReadonlyDoodle>
): NbtEditorActionSnapshot(uid)

@Stable
class PasteActionSnapshot(
    uid: Long,
    val created: SnapshotStateList<ReadonlyDoodle>
): NbtEditorActionSnapshot(uid)

@Stable
sealed class PasteCriteria

@Stable
object CannotBePasted: PasteCriteria()

@Stable
object CanBePastedIntoCompound: PasteCriteria()

@Stable
data class CanBePastedIntoList(val elementsType: TagType): PasteCriteria()

@Stable
data class CanBePastedIntoArray(val arrayTagType: TagType): PasteCriteria()

enum class TagAttribute {
    Named, Unnamed, Value
}

@Stable
class CreateActionSnapshot(
    uid: Long,
    val created: ReadonlyDoodle
): NbtEditorActionSnapshot(uid)

@Stable
class EditActionSnapshot(
    uid: Long,
    val old: ReadonlyDoodle,
    val new: ReadonlyDoodle
): NbtEditorActionSnapshot(uid)

@Stable
class MoveActionSnapshot(
    uid: Long,
    val direction: Direction,
    val moved: SnapshotStateList<ReadonlyDoodle>
): NbtEditorActionSnapshot(uid) {
    enum class Direction {
        Up, Down
    }
}