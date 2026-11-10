package io.github.sembe123.punktjaeger.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Kleine eigene Icons, damit keine zusätzliche Icon-Bibliothek nötig ist. */
object GameIcons {

    val Heart: ImageVector = ImageVector.Builder(
        name = "Heart",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 21f)
            curveTo(6.5f, 17f, 2.5f, 13.5f, 2.5f, 8.8f)
            curveTo(2.5f, 5.6f, 4.9f, 3.5f, 7.6f, 3.5f)
            curveTo(9.4f, 3.5f, 11f, 4.5f, 12f, 6f)
            curveTo(13f, 4.5f, 14.6f, 3.5f, 16.4f, 3.5f)
            curveTo(19.1f, 3.5f, 21.5f, 5.6f, 21.5f, 8.8f)
            curveTo(21.5f, 13.5f, 17.5f, 17f, 12f, 21f)
            close()
        }
    }.build()

    val Pause: ImageVector = ImageVector.Builder(
        name = "Pause",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(6.5f, 5f)
            lineTo(10f, 5f)
            lineTo(10f, 19f)
            lineTo(6.5f, 19f)
            close()
            moveTo(14f, 5f)
            lineTo(17.5f, 5f)
            lineTo(17.5f, 19f)
            lineTo(14f, 19f)
            close()
        }
    }.build()
}
