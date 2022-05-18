package composables.states.editor.world

class DoodleManager(private val root: NbtDoodle) {

    var cached: List<Doodle> = listOf()

    fun create(): List<Doodle> = root.children(true).also { cached = it }

}