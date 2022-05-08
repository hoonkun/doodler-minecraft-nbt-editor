package composables.main

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.BottomAppBar
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import composables.themed.DimensionCategory
import composables.themed.DimensionItem

@Composable
fun WorldEditor(
    worldPath: String
) {
    val scrollState = rememberScrollState()

    MaterialTheme {
        Column (modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                elevation = 7.dp,
                backgroundColor = Color(55, 55, 57),
                contentPadding = PaddingValues(start = 25.dp, top = 15.dp, bottom = 15.dp),
                modifier = Modifier.zIndex(1f)
            ) {
                Text (
                    "Doodler: Minecraft NBT Editor",
                    color = Color.White,
                    fontSize = 32.sp
                )
            }

            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(43, 43, 43))
                    .zIndex(0f)
            ) {
                Box (modifier = Modifier.fillMaxHeight().weight(0.3f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(Color(60, 63, 65))
                            .verticalScroll(scrollState)
                    ) {
                        DimensionCategory("General", initialFolded = false) {
                            GeneralItems()
                        }
                        DimensionCategory("Overworld", initialFolded = false) {
                            DimensionSpecificItems()
                        }
                        DimensionCategory("Nether", "DIM-1", initialFolded = true) {
                            DimensionSpecificItems()
                        }
                        DimensionCategory("TheEnd", "DIM1", initialFolded = true) {
                            DimensionSpecificItems()
                        }
                        Spacer(modifier = Modifier.height(25.dp))
                    }
                    VerticalScrollbar(
                        ScrollbarAdapter(scrollState),
                        style = ScrollbarStyle(
                            100.dp,
                            15.dp,
                            RectangleShape,
                            250,
                            Color(255, 255, 255, 50),
                            Color(255, 255, 255, 100)
                        ),
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.7f)
                ) {

                }
            }

            BottomAppBar(
                elevation = 20.dp,
                backgroundColor = Color(60, 63, 65),
                contentPadding = PaddingValues(top = 0.dp, bottom = 0.dp, start = 25.dp, end = 25.dp),
                modifier = Modifier.height(40.dp).zIndex(1f)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "by kiwicraft",
                    color = Color(255, 255, 255, 180),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun GeneralItems() {
    DimensionItem("World Data", "level.dat")
    DimensionItem("Players", "playerdata/")
    DimensionItem("Statistics", "stats/")
    DimensionItem("Advancements", "advancements/")
}

@Composable
fun DimensionSpecificItems() {
    DimensionItem("Terrain", "region/")
    DimensionItem("Entities", "entities/")
    DimensionItem("Work Block Owners", "poi/")
    DimensionItem("Others", "data/")
}