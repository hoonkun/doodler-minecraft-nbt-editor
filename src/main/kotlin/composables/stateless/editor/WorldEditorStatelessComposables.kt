package composables.stateless.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import composables.themed.*
import java.awt.Desktop
import java.net.URI
import java.util.*


@Composable
fun BoxScope.NoFileSelected(worldName: String) {
    Column (
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            "< world >",
            color = ThemedColor.WhiteOthers,
            fontSize = 29.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            worldName,
            color = Color.White,
            fontSize = 38.sp
        )
        Spacer(modifier = Modifier.height(35.dp))
        Text(
            "Select Tab in Left Area!",
            color = ThemedColor.WhiteSecondary,
            fontSize = 33.sp
        )
        Spacer(modifier = Modifier.height(25.dp))
        WhatIsThis("")
    }
}

@Composable
fun WhatIsThis(link: String) {
    Spacer(modifier = Modifier.height(60.dp))
    Text(
        "What is this?",
        color = ThemedColor.DocumentationDescription,
        fontSize = 25.sp
    )
    Spacer(modifier = Modifier.height(10.dp))
    LinkText(
        "Documentation",
        color = ThemedColor.Link,
        fontSize = 22.sp
    ) {
        openBrowser(link)
    }
}

fun openBrowser(url: String) {
    val osName = System.getProperty("os.name").lowercase(Locale.getDefault())

    val others: () -> Unit = {
        val runtime = Runtime.getRuntime()
        if (osName.contains("mac")) {
            runtime.exec("open $url")
        } else if (osName.contains("nix") || osName.contains("nux")) {
            runtime.exec("xdg-open $url")
        }
    }

    try {
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.BROWSE)) desktop.browse(URI(url))
            else others()
        } else {
            others()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
