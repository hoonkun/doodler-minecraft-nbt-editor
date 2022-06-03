package activator.doodler.doodle.structures

import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshots.SnapshotStateList
import activator.doodler.doodle.ActualDoodle
import activator.doodler.nbt.TagType

@Stable
abstract class DoodleAction(
    val uid: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DoodleAction

        return uid == other.uid
    }

    override fun hashCode(): Int = uid.hashCode()
}

@Stable
class DeleteDoodleAction(
    uid: Long,
    val deleted: SnapshotStateList<ActualDoodle>
): DoodleAction(uid)

@Stable
class PasteDoodleAction(
    uid: Long,
    val created: SnapshotStateList<ActualDoodle>
): DoodleAction(uid)

@Stable
sealed class PasteTarget

@Stable
object CannotBePasted: PasteTarget()

@Stable
object CanBePastedIntoCompound: PasteTarget()

@Stable
data class CanBePastedIntoList(val elementsType: TagType): PasteTarget()

@Stable
data class CanBePastedIntoArray(val arrayTagType: TagType): PasteTarget()

@Stable
class CreateDoodleAction(
    uid: Long,
    val created: ActualDoodle
): DoodleAction(uid)

@Stable
class EditDoodleAction(
    uid: Long,
    val old: ActualDoodle,
    val new: ActualDoodle
): DoodleAction(uid)

@Stable
class MoveDoodleAction(
    uid: Long,
    val direction: DoodleMoveDirection,
    val moved: SnapshotStateList<ActualDoodle>
): DoodleAction(uid) {
    enum class DoodleMoveDirection {
        UP, DOWN
    }
}