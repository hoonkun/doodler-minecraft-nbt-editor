package composable.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import composable.global.ClickableH1
import composable.global.ClickableH4
import doodler.application.structure.DoodlerEditorType
import doodler.theme.DoodlerTheme
import doodler.unit.dp
import doodler.unit.sp

@Composable
fun Intro(
    openSelector: (DoodlerEditorType) -> Unit
) {
    IntroRoot {
        IntroContent {
            OpenWorld()
            OpenStandalone()
        }
        IntroFooter("doodler, synced with 1.18.2", "by @hoon_kiwicraft")
    }
}

@Composable
fun IntroRoot(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(DoodlerTheme.Colors.Background),
        content = content
    )
}

@Composable
fun ColumnScope.IntroContent(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
fun ColumnScope.OpenWorld() {
    ClickableH1(
        text = "Select World!",
        color = DoodlerTheme.Colors.Primary
    ) { }
}

@Composable
fun ColumnScope.OpenStandalone() {
    ClickableH4(
        text = "...or single NBT file",
        color = DoodlerTheme.Colors.Primary
    ) { }
}

@Composable
fun ColumnScope.IntroFooter(
    startText: String,
    endText: String
) {
    Row(
        modifier = Modifier
            .background(DoodlerTheme.Colors.SecondaryBackground)
            .fillMaxWidth()
            .padding(top = 5.dp, bottom = 5.dp, start = 7.dp, end = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IntroFooterText(startText)
        IntroFooterText(endText)
    }
}

@Composable
fun IntroFooterText(
    text: String
) {
    Text(text , fontSize = 8.sp, color = DoodlerTheme.Colors.OnBackground)
}
