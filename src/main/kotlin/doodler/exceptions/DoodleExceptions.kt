package doodler.exceptions

import activator.doodler.doodle.NbtDoodle
import doodler.nbt.TagType

open class DoodleException(
    val title: String,
    val summary: String?,
    val description: String?
): Exception("DOODLE EXCEPTION")

open class InvalidOperationException(
    summary: String?,
    description: String?
): DoodleException("Invalid Operation", summary, description)

class ListElementsTypeMismatchException(
    expected: TagType, actual: TagType
): InvalidOperationException("List element type mismatch", "'$expected' expected, but actual was '$actual'.")

class ValueTypeMismatchException(
    expected: String, actual: String
): InvalidOperationException("Value type mismatch", "'$expected' expected, but actual was '$actual'.")

class InvalidCreationException(
    input: TagType
): InvalidOperationException("Cannot create tag", "$input tag cannot own any child tags.")

class EndCreationException: InvalidOperationException("Cannot create tag", "${TagType.TAG_END} cannot be created manually.")

class NoSelectedItemsException(
    action: String
): InvalidOperationException("Cannot $action", "No elements selected to $action")

class TooManyItemsSelectedException(
    action: String
): InvalidOperationException("Cannot $action", "Cannot $action tags into multiple elements at once.")

class AttemptToEditMultipleTagsException:
    InvalidOperationException("Cannot edit", "Cannot edit multiple tags at once.")

class InvalidPasteTargetException(type: TagType):
    InvalidOperationException("Cannot paste", "These tags can only be pasted into '$type'")

class InvalidValuePasteTargetException(type: TagType):
    InvalidOperationException("Cannot paste", "These values can only be pasted into '$type'")

class PasteTargetTypeMismatchException(
    expected: TagType,
    actual: TagType
): InvalidOperationException("Cannot paste: Tag type mismatch", "Only $expected could be added, but given was $actual")

class NameConflictException(
    action: String,
    conflictName: String,
    index: Int
): InvalidOperationException("Cannot $action", "Tag which name is $conflictName already exists in index $index.")

open class InternalError(
    summary: String?,
    description: String?
): DoodleException("Internal Error", summary, description)

open class InternalAssertionException: InternalError {
    constructor(expected: String, actual: String):
            super("Assertion Error", "'$expected' expected, but actual was '$actual'")
    constructor(expected: List<String>, actual: String):
            super("Assertion Error", "${expected.joinToString(" or ") { "'$it'" }} expected, but actual was '$actual'")
}

class ChildrenInitiationException(
    type: TagType
): InternalError("Cannot initiate child tag", "$type cannot own any child tags.")

class ValueSuffixException(
    type: TagType
): InternalError("Cannot get children size of tag", "$type cannot own any child tags.")

class VirtualActionCancelException(action: String):
    InternalError("Cannot cancel creation", "Target must be selected to cancel $action.")

class ParentNotFoundException:
    InternalAssertionException(NbtDoodle::class.java.simpleName, "Nothing")
