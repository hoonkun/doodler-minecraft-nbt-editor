package composables.stateful.editor.world

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import composables.stateless.editor.*
import doodler.editor.McaEditor
import doodler.editor.StandaloneNbtEditor
import doodler.editor.states.rememberWorldEditorState
import doodler.minecraft.WorldUtils
import doodler.nbt.tag.CompoundTag
import doodler.nbt.tag.StringTag

@Composable
fun WorldEditor(
    worldPath: String
) {
    val states = rememberWorldEditorState()

    if (states.worldSpec.tree == null)
        states.worldSpec.tree = WorldUtils.load(worldPath)

    val levelInfo = WorldUtils.readLevel(states.worldSpec.requireTree.level.readBytes())["Data"]?.getAs<CompoundTag>()

    if (states.worldSpec.name == null)
        states.worldSpec.name = levelInfo!!["LevelName"]
            ?.getAs<StringTag>()
            ?.value

    if (states.worldSpec.tree == null || states.worldSpec.name == null) {
        // TODO: Handle Loading or Parse Error here
        return
    }

    val tree = states.worldSpec.requireTree
    val name = states.worldSpec.requireName

    val onOpenRequest: (OpenRequest) -> Unit = handleRequest@ { request ->
        when (request) {
            is NbtOpenRequest -> {
                if (states.manager.hasItem(request.ident)) {
                    states.manager.select(states.manager[request.ident]!!)
                } else {
                    states.manager.open(StandaloneNbtEditor.fromFile(request.file))
                }
            }
            is AnvilOpenRequest -> {
                if (!states.manager.hasItem("ANVIL_SELECTOR")) {
                    states.manager.open(McaEditor().apply { from = request })
                } else {
                    val selector = states.manager["ANVIL_SELECTOR"] ?: return@handleRequest
                    if (selector !is McaEditor) return@handleRequest

                    if (request is GlobalAnvilInitRequest && selector.globalMcaPayload != null)
                        selector.from = GlobalAnvilUpdateRequest()
                    else
                        selector.from = request

                    states.manager.select(selector)
                }
            }
        }
    }

    MaterialTheme {
        MainColumn {
            TopBar { TopBarText(name) }

            MainArea {
                MainFiles {
                    WorldTreeView(name, tree, onOpenRequest)
                }
                MainContents {
                    if (states.manager.editors.size == 0) {
                        NoFileSelected(name)
                    } else {
                        EditorManager(levelInfo, tree, states.manager)
                    }
                }
            }

            BottomBar {
                Spacer(modifier = Modifier.weight(1f))
                BottomBarText("by kiwicraft")
            }
        }
    }
}