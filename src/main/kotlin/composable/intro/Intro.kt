package composable.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import composable.global.*
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
            IntroStart {
                DefaultH4(text = "doodler: Minecraft NBT Editor")
                PrimaryLinkH1(text = "Select World!") { openSelector(DoodlerEditorType.World) }
                PrimaryLinkH5(text = "...or single NBT file") { openSelector(DoodlerEditorType.Standalone) }
                Spacer(modifier = Modifier.height(20.dp))
            }
            ExternalLinkH5(
                text = "Getting Started?",
                modifier = Modifier.alpha(0.65f).padding(top = 20.dp, bottom = 5.dp).align(Alignment.BottomEnd)
            )
        }
        IntroFooter(startText = "doodler, synced with 1.18.2", endText = "by @hoon_kiwicraft")
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
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(top = 3.dp, bottom = 3.dp, start = 7.dp, end = 7.dp),
        content = content
    )
}

@Composable
fun BoxScope.IntroStart(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
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
