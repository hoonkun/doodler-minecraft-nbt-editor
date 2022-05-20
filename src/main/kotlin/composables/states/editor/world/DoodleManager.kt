package composables.states.editor.world

class DoodleManager(private val root: NbtDoodle) {

    var cached: List<ActualDoodle> = listOf()

    fun create(): List<ActualDoodle> = root.children(true).also { cached = it }

}