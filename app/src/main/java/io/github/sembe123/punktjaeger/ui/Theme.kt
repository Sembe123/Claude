package io.github.sembe123.punktjaeger.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object GameColors {
    val BackgroundTop = Color(0xFF1B1D45)
    val BackgroundBottom = Color(0xFF0B0C1E)
    val Surface = Color(0xFF23254F)
    val TextMuted = Color(0xFFB8BCE6)

    /** Farben der normalen Punkte – Anzahl muss zu GameEngine.PALETTE_SIZE passen. */
    val Palette = listOf(
        Color(0xFF35D7FF),
        Color(0xFFFF4FA3),
        Color(0xFF9BE15D),
        Color(0xFFFFA53D),
        Color(0xFFA77BFF),
    )

    val Gold = Color(0xFFFFD54A)
    val GoldLight = Color(0xFFFFF4C2)
    val GoldDark = Color(0xFFE09A00)
    val Danger = Color(0xFFFF4D4D)
    val BombBody = Color(0xFF2A2B3A)
    val Spark = Color(0xFFFFE08A)
    val Fuse = Color(0xFFB8A27A)
    val Heart = Color(0xFFFF4D6D)

    fun palette(index: Int): Color = Palette[Math.floorMod(index, Palette.size)]
}

val BackgroundBrush = Brush.verticalGradient(listOf(GameColors.BackgroundTop, GameColors.BackgroundBottom))

private val AppColorScheme = darkColorScheme(
    primary = Color(0xFF35D7FF),
    onPrimary = Color(0xFF00222B),
    secondary = Color(0xFFFF4FA3),
    onSecondary = Color.White,
    background = GameColors.BackgroundBottom,
    onBackground = Color.White,
    surface = GameColors.Surface,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2E3163),
    onSurfaceVariant = GameColors.TextMuted,
    outline = Color(0xFF5A5E9A),
)

@Composable
fun PunktjaegerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColorScheme, content = content)
}
