package doodler.logger

import java.util.*

class RecomposeLogger {

    companion object {

        var Enabled = false

        fun log(from: String) {
            if (!Enabled) return
            val h = Calendar.getInstance()[Calendar.HOUR].toString().padStart(2, '0')
            val m = Calendar.getInstance()[Calendar.MINUTE].toString().padStart(2, '0')
            val s = Calendar.getInstance()[Calendar.SECOND].toString().padStart(2, '0')
            val ms = Calendar.getInstance()[Calendar.MILLISECOND].toString().padStart(4, '0')
            println("[$h:$m:$s:$ms] ${"RECOMPOSE".padEnd(12)} :: $from")
        }

    }

}