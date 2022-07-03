package doodler.application.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import doodler.application.structure.*
import doodler.minecraft.structures.WorldSpecification
import java.io.File

@Stable
class DoodlerAppState(
    val exit: () -> Unit
) {

    val windows = mutableStateListOf<DoodlerWindow>(
        IntroDoodlerWindow("doodler")
    )

    fun eraseAll() {
        windows.clear()
        exit()
    }

    fun erase(that: DoodlerWindow) {
        windows.remove(that)
    }

    fun sketch(that: DoodlerWindow): Boolean {
        if (windows.any { it is EditorDoodlerWindow && that is EditorDoodlerWindow && it.path == that.path })
            return false
        windows.add(that)
        return true
    }

    fun restart(callback: () -> Unit) {
        val oldWindows = windows.toList()
        windows.clear()
        callback()
        windows.addAll(oldWindows.map { it.copy() }.sortedBy { if (it is IntroDoodlerWindow) 1 else 0 })
    }

    fun sketchWorldEditor(title: String, path: String, worldSpec: WorldSpecification): Boolean {
        val that = WorldEditorDoodlerWindow(
            title = "doodler - ${title}[${DoodlerEditorType.World.name}]",
            path = path,
            worldSpec = worldSpec
        )

        return sketchEditor(that)
    }

    fun sketchStandaloneEditor(title: String, file: File): Boolean {
        val that = StandaloneEditorDoodlerWindow(
            title = "doodler - ${title}[${DoodlerEditorType.World.name}]",
            file = file
        )

        return sketchEditor(that)
    }

    fun sketchEditor(that: EditorDoodlerWindow): Boolean {
        if (windows.any { it is EditorDoodlerWindow && it.path == that.path })
            return false

        windows.add(that)
        return true
    }

}

@Composable
fun rememberDoodlerApplicationState(exit: () -> Unit) = remember { DoodlerAppState(exit) }