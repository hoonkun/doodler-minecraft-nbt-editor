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
import doodler.unit.ddp
import doodler.unit.dsp

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
                Spacer(modifier = Modifier.height(20.ddp))
            }
            ExternalLinkH5(
                text = "Getting Started?",
                modifier = Modifier.alpha(0.65f).padding(top = 20.ddp, bottom = 5.ddp).align(Alignment.BottomEnd)
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
            .padding(top = 3.ddp, bottom = 3.ddp, start = 7.ddp, end = 7.ddp),
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
            .padding(top = 5.ddp, bottom = 5.ddp, start = 7.ddp, end = 7.ddp),
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
    Text(text , fontSize = 8.dsp, color = DoodlerTheme.Colors.OnBackground)
}
