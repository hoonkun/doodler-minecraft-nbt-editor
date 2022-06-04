package composable.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import composable.global.ClickableH1
import composable.global.ClickableH5
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
            Title()
            PrimaryLinkH1("Select World!")
            PrimaryLinkH4("...or single NBT file")
            DefaultH4(
                "Getting Started?",
                modifier = Modifier.alpha(0.65f).padding(top = 20.dp, bottom = 5.dp)
            )
            ExternalLinkH5("Document")
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
fun ColumnScope.Title() {
    Text(
        "doodler: Minecraft NBT Editor",
        color = DoodlerTheme.Colors.OnBackground,
        modifier = Modifier.alpha(0.75f).padding(bottom = 10.dp)
    )
}

@Composable
fun ColumnScope.DefaultH4(
    text: String,
    modifier: Modifier
) {
    Text(
        text,
        color = DoodlerTheme.Colors.OnBackground,
        fontSize = 12.sp,
        modifier = Modifier.then(modifier)
    )
}

@Composable
fun ColumnScope.ExternalLinkH5(text: String) {
    ClickableH5(text, DoodlerTheme.Colors.ExternalLink) { }
}

@Composable
fun ColumnScope.PrimaryLinkH1(text: String) {
    ClickableH1(
        text = text,
        color = DoodlerTheme.Colors.PrimaryLink
    ) { }
}

@Composable
fun ColumnScope.PrimaryLinkH4(text: String) {
    ClickableH4(
        text = text,
        color = DoodlerTheme.Colors.PrimaryLink
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
