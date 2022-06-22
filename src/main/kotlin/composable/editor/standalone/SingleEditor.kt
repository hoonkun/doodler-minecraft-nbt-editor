package composable.editor.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import composable.editor.NbtEditor
import composable.editor.world.Breadcrumb
import composable.editor.world.BreadcrumbsBar
import doodler.application.structure.StandaloneEditorDoodlerWindow
import doodler.theme.DoodlerTheme
import doodler.unit.ddp


@Composable
fun StandaloneNbtEditor(
    window: StandaloneEditorDoodlerWindow
) {
    Column(modifier = Modifier.fillMaxSize().background(DoodlerTheme.Colors.Background)) {
        BreadcrumbsBar {
            Breadcrumb(window.editor.breadcrumb, disableFirstSlash = true)
            Spacer(modifier = Modifier.width(50.ddp))
        }
        Box(modifier = Modifier.fillMaxSize()) {
            NbtEditor(window.editor)
        }
    }
}