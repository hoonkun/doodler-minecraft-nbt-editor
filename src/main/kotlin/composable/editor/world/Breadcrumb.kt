package composable.editor.world

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import composable.editor.*
import doodler.doodle.structures.*
import doodler.editor.*
import doodler.nbt.TagType
import doodler.theme.DoodlerTheme
import doodler.unit.ddp

@Composable
fun Breadcrumb(
    items: List<Breadcrumb>,
    disableFirstSlash: Boolean = false
) {

    if (!disableFirstSlash) {
        Spacer(modifier = Modifier.width(3.ddp))
        Text("/", color = Color.White, modifier = Modifier.alpha(0.1f))
        Spacer(modifier = Modifier.width(3.ddp))
    }

    for (item in items) {
        when (item) {
            is NbtBreadcrumb -> NbtBreadcrumb(item)
            is MultipleNbtBreadcrumb -> MultipleNbtBreadcrumb(item)
            is McaTypeBreadcrumb -> McaTypeBreadcrumb(item)
            is DimensionBreadcrumb -> DimensionBreadcrumb(item)
            is OtherBreadcrumb -> OtherBreadcrumb(item)
        }
        if (item == items.last()) continue
        Spacer(modifier = Modifier.width(3.ddp))
        Text("/", color = Color.White, modifier = Modifier.alpha(0.1f))
        Spacer(modifier = Modifier.width(3.ddp))
    }

}

@Composable
fun NbtBreadcrumb(item: NbtBreadcrumb) {
    when (val doodle = item.doodle) {
        is TagDoodle -> TagCrumb(doodle)
        is ArrayValueDoodle -> ArrayValueCrumb(doodle)
        is TagCreatorDoodle -> TagCreatorCrumb(doodle)
        is ArrayValueCreatorDoodle -> ArrayValueCreatorCrumb(doodle)
        is TagEditorDoodle -> TagEditorCrumb(doodle)
        is ArrayValueEditorDoodle -> ArrayValueEditorCrumb(doodle)
    }
}

@Composable
fun BreadcrumbTagDoodleType(tagType: TagType) =
    TagDoodleType(type = tagType, backgroundColor = DoodlerTheme.Colors.Breadcrumb.TagTypeBackground)

@Composable
fun BreadcrumbTagIndex(index: Int) =
    ExpandableTagItemDoodleIndex(
        index = index,
        prefix = "[", suffix = "]",
        backgroundColor = Color.Transparent
    )

@Composable
fun TagCrumb(doodle: TagDoodle) {
    BreadcrumbTagDoodleType(doodle.tag.type)
    NbtBreadcrumbSpacer()

    if (doodle.tag.name != null) {
        NbtBreadcrumbSpacer()
        TagDoodleName(doodle.tag.name)
    } else {
        BreadcrumbTagIndex(doodle.index)
        if (doodle.tag.type.let { it.isString() || it.isNumber() }) NbtBreadcrumbSpacer()
        if (doodle.tag.type.isString()) StringTagDoodleValue("*${doodle.value}")
        else if (doodle.tag.type.isNumber()) NumberTagDoodleValue("*${doodle.value}")
    }
}

@Composable
fun ArrayValueCrumb(doodle: ArrayValueDoodle) {
    val type = doodle.parent?.tag?.type?.arrayElementType() ?: return

    BreadcrumbTagDoodleType(type)
    NbtBreadcrumbSpacer()
    BreadcrumbTagIndex(doodle.index)
    NbtBreadcrumbSpacer()
    NumberTagDoodleValue(doodle.value)
}

@Composable
fun TagCreatorCrumb(doodle: TagCreatorDoodle) {
    BreadcrumbTagDoodleType(doodle.type)
    NbtBreadcrumbSpacer()
    TagDoodleName(" ... ")
}

@Composable
fun ArrayValueCreatorCrumb(doodle: ArrayValueCreatorDoodle) {
    BreadcrumbTagDoodleType(doodle.parent.tag.type.arrayElementType())
    NbtBreadcrumbSpacer()
    BreadcrumbTagIndex(doodle.parent.children.size)
    NbtBreadcrumbSpacer()
    TagDoodleName(" ... ")
}

@Composable
fun TagEditorCrumb(doodle: TagEditorDoodle) {
    BreadcrumbTagDoodleType(doodle.source.tag.type)
    NbtBreadcrumbSpacer()
    if (doodle.source.tag.name != null) TagDoodleName("${doodle.source.tag.name} *")
    else {
        BreadcrumbTagIndex(doodle.source.index)
        if (doodle.source.tag.type.let { it.isString() || it.isNumber() }) NbtBreadcrumbSpacer()
        if (doodle.source.tag.type.isString()) StringTagDoodleValue("${doodle.source.value} *")
        else if (doodle.source.tag.type.isNumber()) NumberTagDoodleValue("${doodle.source.value} *")
    }
}

@Composable
fun ArrayValueEditorCrumb(doodle: ArrayValueEditorDoodle) {
    val type = doodle.source.parent?.tag?.type?.arrayElementType() ?: return

    BreadcrumbTagDoodleType(type)
    NbtBreadcrumbSpacer()
    BreadcrumbTagIndex(doodle.source.index)
    NbtBreadcrumbSpacer()
    NumberTagDoodleValue("${doodle.source.value} *")
}

@Composable
fun MultipleNbtBreadcrumb(item: MultipleNbtBreadcrumb) {
    ExpandableTagDoodleValue("${item.count} items selected")
}

@Composable
fun DimensionBreadcrumb(item: DimensionBreadcrumb) {
    BreadcrumbIcon("/icons/breadcrumb/dimension_${item.dimension.displayName}.png")
    TagDoodleName(item.dimension.name)
}

@Composable
fun McaTypeBreadcrumb(item: McaTypeBreadcrumb) {
    BreadcrumbIcon("/icons/breadcrumb/mca_type_${item.type.pathName}.png")
    TagDoodleName(item.type.displayName)
}

@Composable
fun OtherBreadcrumb(item: OtherBreadcrumb) {
    when (item.type) {
        OtherBreadcrumb.Type.WorldMap, OtherBreadcrumb.Type.McaMap ->
            BreadcrumbIcon("/icons/breadcrumb/editor_type_world_map.png", item = true)
        else -> { /* no-op */ }
    }
    TagDoodleName(item.name)
}

@Composable
fun BreadcrumbIcon(path: String, item: Boolean = false) {
    Image(
        painterResource(path),
        contentDescription = null,
        modifier = Modifier.size(if (item) 13.ddp else 10.ddp).clip(RoundedCornerShape(3.ddp))
    )
    Spacer(modifier = Modifier.width(3.ddp))
}

@Composable
fun NbtBreadcrumbSpacer() = Spacer(modifier = Modifier.width(2.ddp))
