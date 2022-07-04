package composable.editor.world

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import composable.global.ClickableText
import doodler.theme.DoodlerTheme
import doodler.unit.ddp
import doodler.unit.dsp
import doodler.utils.BrowserUtils

@Composable
fun BoxScope.EmptyTab() =
    Column(
        modifier = Modifier.align(Alignment.Center).wrapContentWidth()
    ) {
        Column(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(5.ddp, 5.ddp, 0.ddp, 0.ddp))
                .padding(13.ddp)
        ) {
            CommentText("don't know what to open?\njust select 'dimensions' in left hierarchy!")
            DocumentationText("select file to edit content of nbt.\nthese can be found in left file hierarchy.")
            FunctionDeclaration("HelloDoodler") {
                FunctionCall("Hint") {
                    HintText(key = "mca", "set of specific nbt data of chunk", comma = true)
                    HintText(key = "dat", "standalone nbt file")
                }
            }
            EmptyTabText(text = " ", color = DoodlerTheme.Colors.Text.IdeGeneral)
            CommentText("if you want some detailed documentation(Korean), visit here!")
            PropertyText("Documentation", "", false)
            Row {
                EmptyTabText("    ", color = Color.Transparent)
                ClickableText(
                    text = "\"https://github.com/hoonkun/doodler-minecraft-nbt-editor\"",
                    color = DoodlerTheme.Colors.Text.IdeStringLiteral,
                    fontSize = 10.dsp,
                    onClick = { BrowserUtils.open("https://github.com/hoonkun/doodler-minecraft-nbt-editor") }
                )
            }
        }
        Row(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(0.ddp, 0.ddp, 5.ddp, 5.ddp))
                .padding(horizontal = 13.ddp, vertical = 5.ddp)
        ) {
            EmptyTabText(
                // where is `justify-content: stretch;`???
                text = "HelloDoodler.kt                                                ",
                color = DoodlerTheme.Colors.Text.IdeGeneral
            )
        }
    }

@Composable
fun ColumnScope.FunctionDeclaration(text: String, content: @Composable ColumnScope.() -> Unit) {
    EmptyTabText(
        text = AnnotatedString(
            text = "fun $text() =",
            spanStyles = listOf(
                AnnotatedString.Range(
                    item = SpanStyle(color = DoodlerTheme.Colors.Text.IdeKeyword),
                    start = 0,
                    end = 3
                ),
                AnnotatedString.Range(
                    item = SpanStyle(color = DoodlerTheme.Colors.Text.IdeGeneral),
                    start = 4 + text.length,
                    end = 4 + text.length + 4
                )
            )
        ),
        color = DoodlerTheme.Colors.Text.IdeFunctionName
    )
    content()
}

@Composable
fun ColumnScope.FunctionCall(text: String, content: @Composable ColumnScope.() -> Unit) {
    EmptyTabText(
        text = "    $text(",
        color = DoodlerTheme.Colors.Text.IdeGeneral
    )
    content()
    EmptyTabText(
        text = "    )",
        color = DoodlerTheme.Colors.Text.IdeGeneral
    )
}

@Composable
fun HintText(key: String, value: String, comma: Boolean = false) =
    EmptyTabText(
        text = AnnotatedString(
            text = """        $key = "$value"${if (comma) "," else ""}""",
            spanStyles = listOf(
                AnnotatedString.Range(
                    item = SpanStyle(color = DoodlerTheme.Colors.Text.IdeStringLiteral),
                    start = 8 + key.length + 3,
                    end = 8 + key.length + 3 + value.length + 2
                ),
                AnnotatedString.Range(
                    item = SpanStyle(color = DoodlerTheme.Colors.Text.IdeKeyword),
                    start = 8 + key.length + 3 + value.length + 2,
                    end = (8 + key.length + 3 + value.length + 2).let { if (comma) it + 1 else it }
                )
            )
        ),
        color = DoodlerTheme.Colors.Text.IdeFunctionProperty
    )

@Composable
fun ColumnScope.CommentText(
    text: String
) = EmptyTabText(
    text = "// ${text.replace("\n", "\n// ")}",
    color = DoodlerTheme.Colors.Text.IdeComment
)

@Composable
fun ColumnScope.DocumentationText(
    text: String
) = EmptyTabText(
    text = "/* ${text.replace("\n", "\n * ")} */",
    color = DoodlerTheme.Colors.Text.IdeDocumentation
)

@Composable
fun ColumnScope.PropertyText(
    name: String,
    value: String,
    newLine: Boolean
) = EmptyTabText(
    text = AnnotatedString(
        text = """val $name = ${if (newLine) "\n    " else ""}$value""",
        spanStyles = listOf(
            AnnotatedString.Range(
                item = SpanStyle(color = DoodlerTheme.Colors.Text.IdeKeyword),
                start = 0,
                end = 3
            ),
            AnnotatedString.Range(
                item = SpanStyle(color = DoodlerTheme.Colors.Text.IdePropertyName),
                start = 4,
                end = 4 + name.length
            ),
            AnnotatedString.Range(
                item = SpanStyle(color = DoodlerTheme.Colors.Text.IdeStringLiteral),
                start = 4 + name.length + 3,
                end = 4 + name.length + 3 + value.length + (if (newLine) 5 else 0)
            )
        )
    ),
    color = DoodlerTheme.Colors.Text.IdeGeneral
)

@Composable
fun EmptyTabText(
    text: String,
    color: Color
) = EmptyTabText(AnnotatedString(text = text), color = color)

@Composable
fun EmptyTabText(
    text: AnnotatedString,
    color: Color
) = Text(
    text = text,
    fontSize = 10.dsp,
    color = color,
    lineHeight = 20.dsp
)
