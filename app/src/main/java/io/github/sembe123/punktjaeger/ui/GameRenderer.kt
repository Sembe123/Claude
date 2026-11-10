package io.github.sembe123.punktjaeger.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sembe123.punktjaeger.game.Effect
import io.github.sembe123.punktjaeger.game.EffectKind
import io.github.sembe123.punktjaeger.game.Target
import io.github.sembe123.punktjaeger.game.TargetKind
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private const val TWO_PI = (2 * PI).toFloat()

fun DrawScope.drawTarget(target: Target, time: Float) {
    val center = Offset(target.x, target.y)
    when (target.kind) {
        TargetKind.NORMAL ->
            drawNormalTarget(center, target.radius, GameColors.palette(target.colorIndex), target.remaining)
        TargetKind.GOLD -> drawGoldTarget(center, target.radius, target.remaining, time)
        TargetKind.BOMB -> drawBomb(center, target.radius, time + target.id * 0.37f)
    }
}

fun DrawScope.drawNormalTarget(center: Offset, radius: Float, color: Color, remaining: Float) {
    if (radius <= 0.5f) return
    drawCircle(color = color.copy(alpha = 0.16f), radius = radius * 1.4f, center = center)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(lerp(color, Color.White, 0.55f), color, lerp(color, Color.Black, 0.3f)),
            center = center + Offset(-radius * 0.35f, -radius * 0.35f),
            radius = radius * 1.7f,
        ),
        radius = radius,
        center = center,
    )
    drawTimerRing(center, radius, remaining, Color.White)
}

fun DrawScope.drawGoldTarget(center: Offset, radius: Float, remaining: Float, time: Float) {
    if (radius <= 0.5f) return
    val glow = 0.22f + 0.12f * sin(time * 10f)
    drawCircle(color = GameColors.Gold.copy(alpha = glow), radius = radius * 1.5f, center = center)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(GameColors.GoldLight, GameColors.Gold, GameColors.GoldDark),
            center = center + Offset(-radius * 0.3f, -radius * 0.3f),
            radius = radius * 1.6f,
        ),
        radius = radius,
        center = center,
    )
    drawPath(starPath(center, radius * 0.58f, radius * 0.25f, time * 1.5f), color = Color.White)
    drawTimerRing(center, radius, remaining, GameColors.GoldLight)
}

fun DrawScope.drawBomb(center: Offset, radius: Float, time: Float) {
    if (radius <= 0.5f) return
    val pulse = 0.5f + 0.5f * sin(time * 9f)
    drawCircle(color = GameColors.Danger.copy(alpha = 0.10f + 0.16f * pulse), radius = radius * 1.45f, center = center)

    val fuseStart = center + Offset(radius * 0.45f, -radius * 0.75f)
    val fuseEnd = center + Offset(radius * 0.85f, -radius * 1.15f)
    drawLine(
        color = GameColors.Fuse,
        start = fuseStart,
        end = fuseEnd,
        strokeWidth = radius * 0.13f,
        cap = StrokeCap.Round,
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF55576B), GameColors.BombBody, Color(0xFF15151D)),
            center = center + Offset(-radius * 0.35f, -radius * 0.35f),
            radius = radius * 1.6f,
        ),
        radius = radius,
        center = center,
    )
    drawCircle(
        color = GameColors.Danger.copy(alpha = 0.6f + 0.4f * pulse),
        radius = radius,
        center = center,
        style = Stroke(width = radius * 0.1f),
    )

    val c = radius * 0.36f
    val crossWidth = radius * 0.17f
    drawLine(GameColors.Danger, center + Offset(-c, -c), center + Offset(c, c), crossWidth, StrokeCap.Round)
    drawLine(GameColors.Danger, center + Offset(c, -c), center + Offset(-c, c), crossWidth, StrokeCap.Round)

    drawCircle(color = Color(0xFFFF8A3D).copy(alpha = 0.55f), radius = radius * (0.22f + 0.1f * pulse), center = fuseEnd)
    drawCircle(color = GameColors.Spark, radius = radius * (0.12f + 0.06f * pulse), center = fuseEnd)
}

fun DrawScope.drawEffect(effect: Effect, textMeasurer: TextMeasurer) {
    val p = effect.progress
    val fade = 1f - p
    val center = Offset(effect.x, effect.y)
    val r = max(effect.radius, 12.dp.toPx())

    when (effect.kind) {
        EffectKind.HIT -> {
            val color = GameColors.palette(effect.colorIndex)
            drawBurst(center, r, p, color, rays = 8)
            drawFloatingText(textMeasurer, "+${effect.points}", center, p, Color.White, 26.sp)
        }

        EffectKind.GOLD -> {
            drawBurst(center, r * 1.2f, p, GameColors.Gold, rays = 12)
            drawFloatingText(textMeasurer, "+${effect.points}", center, p, GameColors.Gold, 32.sp)
            if (effect.healed) {
                drawFloatingText(
                    textMeasurer,
                    "+1 Herz",
                    center + Offset(0f, 36.dp.toPx()),
                    p,
                    GameColors.Heart,
                    18.sp,
                )
            }
        }

        EffectKind.BOMB -> {
            drawCircle(
                color = Color(0xFFFF8A3D),
                radius = r * (1f + 2.2f * p),
                center = center,
                alpha = fade * 0.5f,
            )
            drawBurst(center, r * 1.4f, p, GameColors.Danger, rays = 14)
            drawFloatingText(textMeasurer, "Autsch!", center, p, GameColors.Danger, 28.sp)
        }

        EffectKind.MISS -> {
            drawCircle(
                color = GameColors.Danger,
                radius = r * (1f - 0.5f * p),
                center = center,
                alpha = fade,
                style = Stroke(width = 3.dp.toPx()),
            )
            drawFloatingText(textMeasurer, "Verpasst!", center + Offset(0f, -r * 1.6f), p, GameColors.Danger, 20.sp)
        }

        EffectKind.EMPTY_TAP -> drawCircle(
            color = Color.White,
            radius = r * (0.4f + 0.6f * p),
            center = center,
            alpha = fade * 0.35f,
            style = Stroke(width = 2.dp.toPx()),
        )

        EffectKind.FADE -> drawCircle(
            color = Color.White,
            radius = r * (1f - 0.6f * p),
            center = center,
            alpha = fade * 0.25f,
        )
    }
}

/** Ausdehnender Ring mit davonfliegenden Funken. */
private fun DrawScope.drawBurst(center: Offset, radius: Float, progress: Float, color: Color, rays: Int) {
    val fade = 1f - progress
    drawCircle(
        color = color,
        radius = radius * (1f + 0.9f * progress),
        center = center,
        alpha = fade,
        style = Stroke(width = radius * 0.16f * fade + 1f),
    )
    val distance = radius * (0.9f + 1.5f * progress)
    val dotRadius = radius * 0.11f * fade
    if (dotRadius < 0.5f) return
    for (i in 0 until rays) {
        val angle = i * TWO_PI / rays + progress * 0.6f
        drawCircle(
            color = color,
            radius = dotRadius,
            center = center + Offset(cos(angle) * distance, sin(angle) * distance),
            alpha = fade,
        )
    }
}

private fun DrawScope.drawFloatingText(
    textMeasurer: TextMeasurer,
    text: String,
    center: Offset,
    progress: Float,
    color: Color,
    fontSize: TextUnit,
) {
    val layout = textMeasurer.measure(
        text = AnnotatedString(text),
        style = TextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), offset = Offset(0f, 2f), blurRadius = 6f),
        ),
    )
    val rise = 56.dp.toPx() * progress
    val alpha = if (progress < 0.6f) 1f else ((1f - progress) / 0.4f).coerceIn(0f, 1f)
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(
            center.x - layout.size.width / 2f,
            center.y - layout.size.height / 2f - rise,
        ),
        alpha = alpha,
    )
}

/** Kreisbogen, der die verbleibende Zeit eines Punktes anzeigt. */
private fun DrawScope.drawTimerRing(center: Offset, radius: Float, remaining: Float, color: Color) {
    val stroke = max(3f, radius * 0.11f)
    val ringRadius = radius + stroke * 1.3f
    drawArc(
        color = color.copy(alpha = 0.85f),
        startAngle = -90f,
        sweepAngle = 360f * remaining.coerceIn(0f, 1f),
        useCenter = false,
        topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
        size = Size(ringRadius * 2f, ringRadius * 2f),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
}

private fun starPath(center: Offset, outer: Float, inner: Float, rotation: Float): Path {
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val angle = rotation - PI.toFloat() / 2f + i * PI.toFloat() / 5f
        val x = center.x + cos(angle) * r
        val y = center.y + sin(angle) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
