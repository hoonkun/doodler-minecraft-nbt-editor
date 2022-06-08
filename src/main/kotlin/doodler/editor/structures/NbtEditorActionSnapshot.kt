package doodler.editor.structures

import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import doodler.nbt.TagType
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
class ReadonlyDoodleSnapshot(
    val doodle: ReadonlyDoodle,
    val index: Int
)

fun ReadonlyDoodle.snapshot() = ReadonlyDoodleSnapshot(this, this.index)

fun SnapshotStateList<ReadonlyDoodle>.snapshot() = map { it.snapshot() }.toMutableStateList()

@Stable
class DeleteActionSnapshot(
    uid: Long,
    val deleted: SnapshotStateList<ReadonlyDoodleSnapshot>
): NbtEditorActionSnapshot(uid)

@Stable
class PasteActionSnapshot(
    uid: Long,
    val created: SnapshotStateList<ReadonlyDoodleSnapshot>
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
    val created: ReadonlyDoodleSnapshot
): NbtEditorActionSnapshot(uid)

@Stable
class EditActionSnapshot(
    uid: Long,
    val old: ReadonlyDoodleSnapshot,
    val new: ReadonlyDoodleSnapshot
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