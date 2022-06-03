package activator.doodler.logger

import java.util.*

class DoodlerLogger {

    companion object {

        private const val tagLength = 12

        private const val TEST_TAG = "TEST"
        private const val RECOMPOSITION_TAG = "RECOMPOSITION"

        private val tagFilters = mutableSetOf<String>(TEST_TAG, RECOMPOSITION_TAG)
        private val messageFilters = mutableSetOf<String>()

        private fun now(): String {
            val h = Calendar.getInstance()[Calendar.HOUR].toString().padStart(2, '0')
            val m = Calendar.getInstance()[Calendar.MINUTE].toString().padStart(2, '0')
            val s = Calendar.getInstance()[Calendar.SECOND].toString().padStart(2, '0')
            val ms = Calendar.getInstance()[Calendar.MILLISECOND].toString().padStart(4, '0')
            return "[$h:$m:$s:$ms]"
        }

        fun log(tag: String, message: String) {
            println("${now()} ${tag.padEnd(tagLength)} :: $message")
        }

        fun test(message: String) {
            if (!tagFilters.contains(TEST_TAG) || messageFilters.find { message.contains(it) } != null) return

            log(TEST_TAG, message)
        }

        fun recomposition(from: String) {
            if (!tagFilters.contains(RECOMPOSITION_TAG) || messageFilters.find { from.contains(it) } != null) return

            log(RECOMPOSITION_TAG, from)
        }

    }

}