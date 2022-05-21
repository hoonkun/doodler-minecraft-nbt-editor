package composables.states.editor.world.extensions

fun <T>MutableList<T>.replaceAt(index: Int, to: T) {
    removeAt(index)
    add(index, to)
}