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
import composables.states.editor.world.Species
import composables.states.editor.world.SpeciesHolder
import doodler.file.WorldDimension
import doodler.file.WorldTree


fun createCategories(tree: WorldTree): List<PhylumCategoryData> {
    val generalItems: (String) -> List<PhylumCategoryItemData> = {
        listOf(
            PhylumCategoryItemData(it, SpeciesHolder.Type.Single, Species.Format.DAT, Species.ContentType.LEVEL),
            PhylumCategoryItemData(it, SpeciesHolder.Type.Multiple, Species.Format.DAT, Species.ContentType.PLAYER),
            PhylumCategoryItemData(it, SpeciesHolder.Type.Multiple, Species.Format.DAT, Species.ContentType.STATISTICS),
            PhylumCategoryItemData(it, SpeciesHolder.Type.Multiple, Species.Format.DAT, Species.ContentType.ADVANCEMENTS)
        )
    }

    val dimensionItems: (WorldDimension) -> List<PhylumCategoryItemData> = {
        val holderType = SpeciesHolder.Type.Multiple
        val prefix = it.displayName
        val result = mutableListOf<PhylumCategoryItemData>()
        val extra: Map<String, Any> = mapOf("dimension" to it)

        if (tree[it].region.isNotEmpty())
            result.add(PhylumCategoryItemData(prefix, holderType, Species.Format.MCA, Species.ContentType.TERRAIN, extra))
        if (tree[it].entities.isNotEmpty())
            result.add(PhylumCategoryItemData(prefix, holderType, Species.Format.MCA, Species.ContentType.ENTITY, extra))
        if (tree[it].poi.isNotEmpty())
            result.add(PhylumCategoryItemData(prefix, holderType, Species.Format.MCA, Species.ContentType.POI, extra))
        if (tree[it].data.isNotEmpty())
            result.add(PhylumCategoryItemData(prefix, holderType, Species.Format.DAT, Species.ContentType.OTHERS, extra))

        result
    }

    return listOf(
        PhylumCategoryData("General", false, generalItems("General")),
        PhylumCategoryData.withDimension(WorldDimension.OVERWORLD, false, dimensionItems),
        PhylumCategoryData.withDimension(WorldDimension.NETHER, factory = dimensionItems),
        PhylumCategoryData.withDimension(WorldDimension.THE_END, factory = dimensionItems)
    )
}

@Composable
fun ColumnScope.PhylumCategory(
    data: PhylumCategoryData,
    content: @Composable ColumnScope.() -> Unit = { }
) {
    var folded by remember { mutableStateOf(data.defaultFolded) }

    Column {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(start = 25.dp, end = 25.dp, top = 30.dp, bottom = 10.dp)
        ) {
            Text(data.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            if (data.description != null) {
                Text(
                    " ${data.description}", fontSize = 16.sp, color = ThemedColor.WhiteOthers,
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
fun ColumnScope.PhylumCategoryItem(
    data: PhylumCategoryItemData,
    selected: String,
    onClick: (PhylumCategoryItemData) -> Unit = { }
) {
    val key = data.key
    ListItem (selected == key, onClick = { onClick(data) }) {
        Row(horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth().padding(start = 25.dp, end = 25.dp)) {
            Text(data.contentType.displayName, fontSize = 24.sp, color = ThemedColor.WhiteSecondary)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                " ${data.contentType.description}", fontSize = 16.sp, color = ThemedColor.WhiteOthers,
                modifier = Modifier.align(Alignment.Bottom).padding(start = 7.dp)
            )
        }
    }
}

class PhylumCategoryData(
    val name: String,
    val defaultFolded: Boolean,
    val items: List<PhylumCategoryItemData>
) {
    companion object {
        fun withDimension(
            dimension: WorldDimension,
            defaultFolded: Boolean = true,
            factory: (WorldDimension) -> List<PhylumCategoryItemData>
        ) = PhylumCategoryData(dimension.displayName, defaultFolded, factory(dimension)).withDescription(dimension.ident)
    }

    var description: String? = null
        private set

    fun withDescription(description: String): PhylumCategoryData {
        this.description = description
        return this
    }
}

class PhylumCategoryItemData (
    parent: String,
    val holderType: SpeciesHolder.Type,
    val format: Species.Format,
    val contentType: Species.ContentType,
    val extras: Map<String, Any> = mapOf()
) {
    val key = "$parent/${contentType.displayName}"
}
