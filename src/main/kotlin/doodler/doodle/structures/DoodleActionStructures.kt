package doodler.doodle.structures

import doodler.doodle.ActualDoodle
import doodler.nbt.TagType

abstract class DoodleAction(
    val uid: Long
)
class DeleteDoodleAction(
    uid: Long,
    val deleted: List<ActualDoodle>
): DoodleAction(uid)

class PasteDoodleAction(
    uid: Long,
    val created: List<ActualDoodle>
): DoodleAction(uid)

sealed class PasteTarget
object CannotBePasted: PasteTarget()
object CanBePastedIntoCompound: PasteTarget()
data class CanBePastedIntoList(val elementsType: TagType): PasteTarget()
data class CanBePastedIntoArray(val arrayTagType: TagType): PasteTarget()

class CreateDoodleAction(
    uid: Long,
    val created: ActualDoodle
): DoodleAction(uid)

class EditDoodleAction(
    uid: Long,
    val old: ActualDoodle,
    val new: ActualDoodle
): DoodleAction(uid)

class MoveDoodleAction(
    uid: Long,
    val direction: DoodleMoveDirection,
    val moved: List<ActualDoodle>
): DoodleAction(uid) {
    enum class DoodleMoveDirection {
        UP, DOWN
    }
}