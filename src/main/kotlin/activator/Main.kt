package activator

import androidx.compose.runtime.*
//import androidx.compose.ui.window.application
import androidx.compose.ui.input.key.Key
//import activator.composables.main.DoodlerWindow
//import activator.doodler.application.states.DoodlerApplicationState

val keys = mutableStateListOf<Key>()

//fun main() = application {
//    val applicationState = remember { DoodlerApplicationState() }
//
//    for (window in applicationState.windows) {
//        key(window) {
//            DoodlerWindow(applicationState, window)
//        }
//    }
//}
