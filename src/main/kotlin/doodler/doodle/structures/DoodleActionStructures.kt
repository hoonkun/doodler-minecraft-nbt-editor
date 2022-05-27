package doodler.doodle.structures

import doodler.doodle.ActualDoodle
import doodler.nbt.TagType

abstract class DoodleAction
class DeleteDoodleAction(
    val deleted: List<ActualDoodle>
): DoodleAction()

class PasteDoodleAction(
    val created: List<ActualDoodle>
): DoodleAction()

sealed class PasteTarget
object CannotBePasted: PasteTarget()
object CanBePastedIntoCompound: PasteTarget()
data class CanBePastedIntoList(val elementsType: TagType): PasteTarget()
data class CanBePastedIntoArray(val arrayTagType: TagType): PasteTarget()

class CreateDoodleAction(
    val created: ActualDoodle
): DoodleAction()

class EditDoodleAction(
    val old: ActualDoodle,
    val new: ActualDoodle
): DoodleAction()

class MoveDoodleAction(
    val direction: DoodleMoveDirection,
    val moved: List<ActualDoodle>
): DoodleAction() {
    enum class DoodleMoveDirection {
        UP, DOWN
    }
}