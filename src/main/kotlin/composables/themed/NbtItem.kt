package composables.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import doodler.doodle.Doodle
import doodler.doodle.NbtDoodle
import doodler.doodle.PrimitiveValueDoodle
import nbt.TagType

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ItemRoot(onClick: () -> Unit = { }, content: @Composable RowScope.() -> Unit) {
    var hover by remember { mutableStateOf(false) }
    var press by remember { mutableStateOf(false) }

    Box (
        modifier = Modifier
            .onPointerEvent(PointerEventType.Press) { press = true }
            .onPointerEvent(PointerEventType.Release) { press = false; onClick() }
            .onPointerEvent(PointerEventType.Enter) { hover = true }
            .onPointerEvent(PointerEventType.Exit) { hover = false }
            .background(ThemedColor.selectable(false, press, hover))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 10.dp)
        ) {
            content()
        }
    }

}

@Composable
private fun ItemText(text: String, color: Color, fontSize: TextUnit = 20.sp) {
    Text(text, color = color, fontFamily = JetBrainsMono, fontSize = fontSize)
}

@Composable
private fun Key(key: String) {
    ItemText(key, Color(169, 183, 198))
}

@Composable
private fun NumberValue(value: String) {
    ItemText(value, Color(104, 151, 187))
}

@Composable
private fun StringValue(value: String) {
    ItemText(value, Color(106, 135, 89))
}

@Composable
private fun ExpandableValue(value: String) {
    Box (
        modifier = Modifier
            .wrapContentSize()
            .background(Color(60, 60, 60), shape = RoundedCornerShape(5.dp))
            .padding(top = 2.dp, bottom = 2.dp, start = 10.dp, end = 10.dp)
    ) {
        ItemText(value, Color(150, 150, 150), 15.sp)
    }
}

@Composable
private fun Index(index: Int) {
    Box (
        modifier = Modifier
            .wrapContentSize()
            .background(Color(60, 60, 60), shape = RoundedCornerShape(5.dp))
            .padding(top = 2.dp, bottom = 2.dp, start = 5.dp, end = 5.dp)
    ) {
        ItemText("$index:", Color(150, 150, 150), 15.sp)
    }
}

@Composable
private fun KeyValue(type: TagType, key: String?, value: String, index: Int) {
    if (key != null) {
        Key(key)
        Spacer(modifier = Modifier.width(20.dp))
    }
    if (index >= 0) {
        Index(index)
        Spacer(modifier = Modifier.width(10.dp))
    }
    when (type) {
        TagType.TAG_BYTE,
            TagType.TAG_DOUBLE,
            TagType.TAG_FLOAT,
            TagType.TAG_INT,
            TagType.TAG_LONG,
            TagType.TAG_SHORT -> NumberValue(value)
        TagType.TAG_STRING -> StringValue(value)
        TagType.TAG_COMPOUND,
            TagType.TAG_BYTE_ARRAY,
            TagType.TAG_INT_ARRAY,
            TagType.TAG_LONG_ARRAY,
            TagType.TAG_LIST -> ExpandableValue(value)
        TagType.TAG_END -> { /* impossible */ }
    }
}

@Composable
fun NbtItem(doodle: Doodle, onClick: () -> Unit = { }) {
    ItemRoot(onClick) {
        Row (
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = (20 + doodle.depth * 50).dp)
        ) {
            when (doodle) {
                is NbtDoodle -> {
                    // Key("${doodle.type}")
                    Spacer(modifier = Modifier.width(20.dp))
                    KeyValue(doodle.type, doodle.name, doodle.value, doodle.index)
                }
                is PrimitiveValueDoodle -> {
                    Index(doodle.index)
                    Spacer(modifier = Modifier.width(10.dp))
                    NumberValue(doodle.value)
                }
            }
        }
    }
}