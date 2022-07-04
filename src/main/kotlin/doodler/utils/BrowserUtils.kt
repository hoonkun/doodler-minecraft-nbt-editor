package doodler.utils

import java.awt.Desktop
import java.net.URI

class BrowserUtils {

    companion object {

        fun open(url: String) {

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
            } else {
                val runtime = Runtime.getRuntime()
                try { runtime.exec("xdg-open $url") } catch (e: Exception) { e.printStackTrace() }
            }

        }

    }

}