package doodler.extension

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlin.coroutines.cancellation.CancellationException

fun CoroutineScope.throwIfInactive()  {
    if (!isActive) throw CancellationException()
}