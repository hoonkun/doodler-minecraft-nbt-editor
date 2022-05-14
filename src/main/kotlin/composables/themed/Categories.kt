package composables.themed

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import composables.states.holder.Species
import composables.states.holder.SpeciesHolder

@Composable
fun ColumnScope.FilesCategory(
    data: CategoryData,
    content: @Composable ColumnScope.() -> Unit = { }
) {
    var folded by remember { mutableStateOf(data.defaultFoldState) }

    Column {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(start = 25.dp, end = 25.dp, top = 30.dp, bottom = 10.dp)
        ) {
            Text(data.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            if (data.description != null) {
                Text(
                    " ${data.description}", fontSize = 16.sp, color = Color(255, 255, 255, 125),
                    modifier = Modifier.align(Alignment.Bottom).padding(start = 7.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            LinkText(
                if (folded) "unfold" else "fold",
                ThemedColor.Bright,
                19.sp
            ) { folded = !folded }
        }
        if (!folded) content()
    }
}


@Composable
fun ColumnScope.FileCategoryItem(
    data: CategoryItemData,
    selected: String,
    onClick: (CategoryItemData) -> Unit = { }
) {
    val key = data.key
    ListItem (selected == key, onClick = { onClick(data) }) {
        Row(horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth().padding(start = 25.dp, end = 25.dp)) {
            Text(data.contentType.displayName, fontSize = 24.sp, color = Color(255, 255, 255, 200))
            Spacer(modifier = Modifier.weight(1f))
            Text(
                " ${data.contentType.description}", fontSize = 16.sp, color = Color(255, 255, 255, 100),
                modifier = Modifier.align(Alignment.Bottom).padding(start = 7.dp)
            )
        }
    }
}

class CategoryData(
    val name: String,
    val defaultFoldState: Boolean,
    val items: List<CategoryItemData>
) {
    var description: String? = null
        private set

    fun withDescription(description: String): CategoryData {
        this.description = description
        return this
    }
}

class CategoryItemData (
    parent: String,
    val holderType: SpeciesHolder.Type,
    val format: Species.Format,
    val contentType: Species.ContentType,
    val extra: Map<String, String> = mapOf()
) {
    val key = "$parent/${contentType.displayName}"
}