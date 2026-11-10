package io.github.sembe123.punktjaeger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sembe123.punktjaeger.GameResult
import io.github.sembe123.punktjaeger.GameViewModel
import io.github.sembe123.punktjaeger.Screen
import io.github.sembe123.punktjaeger.game.GameEngine
import io.github.sembe123.punktjaeger.game.GameEvent
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private val HudHeight = 64.dp
private val ContentMaxWidth = 420.dp
private const val MAX_FRAME_TIME = 0.05f

@Composable
fun PunktjaegerApp(viewModel: GameViewModel) {
    Box(
        Modifier
            .fillMaxSize()
            .background(BackgroundBrush),
    ) {
        when (viewModel.screen) {
            Screen.MENU -> MenuScreen(highscore = viewModel.highscore, onPlay = viewModel::startGame)
            Screen.PLAYING, Screen.GAME_OVER -> GameScreen(viewModel)
        }
    }
}

// ---------------------------------------------------------------------------
// Menü
// ---------------------------------------------------------------------------

@Composable
private fun MenuScreen(highscore: Int, onPlay: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Logo(Modifier.size(width = 220.dp, height = 130.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Punktjäger",
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tippe die Punkte an, bevor sie verschwinden!",
            style = MaterialTheme.typography.bodyLarge,
            color = GameColors.TextMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Row(
                Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "REKORD",
                    style = MaterialTheme.typography.labelMedium,
                    color = GameColors.TextMuted,
                    letterSpacing = 1.5.sp,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "$highscore",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = GameColors.Gold,
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        PrimaryButton(text = "Spielen", onClick = onPlay, modifier = Modifier.widthIn(max = ContentMaxWidth))
        Spacer(Modifier.height(36.dp))

        Column(
            Modifier
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "So geht's",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            RuleRow(
                icon = { DrawnIcon { drawNormalTarget(center, size.minDimension * 0.34f, GameColors.Palette[0], 0.7f) } },
                title = "Bunte Punkte",
                text = "Antippen bringt 10 Punkte. Lässt du einen verschwinden, verlierst du ein Herz.",
            )
            RuleRow(
                icon = { DrawnIcon { drawGoldTarget(center, size.minDimension * 0.3f, 0.7f, 0f) } },
                title = "Goldene Sterne",
                text = "Selten und schnell wieder weg: 50 Punkte und ein Herz zurück.",
            )
            RuleRow(
                icon = { DrawnIcon { drawBomb(center, size.minDimension * 0.3f, 0f) } },
                title = "Bomben",
                text = "Finger weg! Antippen kostet ein Herz – einfach liegen lassen.",
            )
            RuleRow(
                icon = {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Text("×5", fontSize = 22.sp, fontWeight = FontWeight.Black, color = GameColors.Gold)
                    }
                },
                title = "Serie",
                text = "Alle 10 Treffer in Folge steigt dein Multiplikator, bis ×5. " +
                    "Daneben tippen beendet die Serie.",
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Logo(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val unit = size.height
        drawNormalTarget(Offset(size.width * 0.5f, size.height * 0.52f), unit * 0.29f, GameColors.Palette[0], 0.72f)
        drawNormalTarget(Offset(size.width * 0.16f, size.height * 0.3f), unit * 0.13f, GameColors.Palette[1], 0.45f)
        drawGoldTarget(Offset(size.width * 0.84f, size.height * 0.66f), unit * 0.13f, 0.9f, 0.3f)
        drawNormalTarget(Offset(size.width * 0.82f, size.height * 0.17f), unit * 0.07f, GameColors.Palette[2], 0.3f)
        drawNormalTarget(Offset(size.width * 0.2f, size.height * 0.82f), unit * 0.07f, GameColors.Palette[3], 0.8f)
    }
}

@Composable
private fun DrawnIcon(onDraw: DrawScope.() -> Unit) {
    Canvas(Modifier.size(48.dp), onDraw = onDraw)
}

@Composable
private fun RuleRow(icon: @Composable () -> Unit, title: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = GameColors.TextMuted)
        }
    }
}

// ---------------------------------------------------------------------------
// Spiel
// ---------------------------------------------------------------------------

@Composable
private fun GameScreen(vm: GameViewModel) {
    val haptics = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val safeDrawing = WindowInsets.safeDrawing
    val insetLeft = safeDrawing.getLeft(density, layoutDirection)
    val insetTop = safeDrawing.getTop(density)
    val insetRight = safeDrawing.getRight(density, layoutDirection)
    val insetBottom = safeDrawing.getBottom(density)

    // Bildschirm während des Spiels nicht ausgehen lassen.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    BackHandler {
        when {
            vm.screen == Screen.GAME_OVER -> vm.backToMenu()
            vm.paused -> vm.resume()
            else -> vm.pause()
        }
    }

    // Spielschleife: läuft synchron zur Bildwiederholrate des Displays.
    LaunchedEffect(vm) {
        var lastFrame = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (lastFrame == 0L) 0f else ((now - lastFrame) / 1_000_000_000f).coerceAtMost(MAX_FRAME_TIME)
                lastFrame = now
                for (event in vm.step(dt)) {
                    when (event) {
                        GameEvent.BOMB, GameEvent.MISS, GameEvent.GAME_OVER ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        GameEvent.HIT, GameEvent.GOLD -> Unit
                    }
                }
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        LaunchedEffect(width, height, insetLeft, insetTop, insetRight, insetBottom) {
            with(density) {
                val margin = 12.dp.toPx()
                val left = insetLeft + margin
                val top = insetTop + HudHeight.toPx() + margin
                val right = width - insetRight - margin
                val bottom = height - insetBottom - margin
                val baseRadius = max(min(right - left, bottom - top) * 0.09f, 28.dp.toPx())
                vm.engine.setBounds(left, top, right, bottom, baseRadius, 26.dp.toPx())
            }
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(vm) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            // Jeder Finger zählt einzeln – so geht auch Tippen mit zwei Händen.
                            for (change in event.changes) {
                                if (!change.changedToDown()) continue
                                change.consume()
                                // Tipps in die Punkteleiste sollen die Serie nicht beenden.
                                val playAreaTop = vm.engine.top - vm.engine.minTouchRadius
                                if (change.position.y >= playAreaTop) {
                                    vm.tap(change.position.x, change.position.y)
                                }
                            }
                        }
                    }
                },
        ) {
            val time = vm.animationTime
            val engine = vm.engine
            val shakePx = 9.dp.toPx() * (engine.shake / GameEngine.BOMB_SHAKE).coerceIn(0f, 1f)
            translate(left = sin(time * 87f) * shakePx, top = cos(time * 71f) * shakePx) {
                for (target in engine.targets) drawTarget(target, time)
                for (effect in engine.effects) drawEffect(effect, textMeasurer)
            }
        }

        Hud(
            vm = vm,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .fillMaxWidth()
                .height(HudHeight),
        )

        if (vm.paused) {
            PauseOverlay(onResume = vm::resume, onQuit = vm::backToMenu)
        }
        val result = vm.result
        if (vm.screen == Screen.GAME_OVER && result != null) {
            GameOverOverlay(result = result, highscore = vm.highscore, onRetry = vm::startGame, onMenu = vm::backToMenu)
        }
    }
}

@Composable
private fun Hud(vm: GameViewModel, modifier: Modifier = Modifier) {
    Row(
        modifier.padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "PUNKTE",
                style = MaterialTheme.typography.labelSmall,
                color = GameColors.TextMuted,
                letterSpacing = 1.5.sp,
            )
            Text(
                text = "${vm.score}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
        }
        Box(Modifier.width(88.dp), contentAlignment = Alignment.Center) {
            if (vm.combo >= 2) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "×${vm.multiplier}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = if (vm.multiplier > 1) GameColors.Gold else Color.White,
                    )
                    Text(
                        text = "Serie ${vm.combo}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GameColors.TextMuted,
                    )
                }
            }
        }
        Row(
            Modifier.semantics(mergeDescendants = true) { contentDescription = "Leben: ${vm.lives}" },
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            repeat(GameEngine.MAX_LIVES) { i ->
                Icon(
                    imageVector = GameIcons.Heart,
                    contentDescription = null,
                    tint = if (i < vm.lives) GameColors.Heart else Color.White.copy(alpha = 0.18f),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        IconButton(onClick = vm::pause) {
            Icon(imageVector = GameIcons.Pause, contentDescription = "Pause", tint = Color.White)
        }
    }
}

// ---------------------------------------------------------------------------
// Pause und Spielende
// ---------------------------------------------------------------------------

@Composable
private fun PauseOverlay(onResume: () -> Unit, onQuit: () -> Unit) {
    Overlay {
        Text("Pause", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(24.dp))
        PrimaryButton(text = "Weiter", onClick = onResume)
        Spacer(Modifier.height(12.dp))
        SecondaryButton(text = "Spiel beenden", onClick = onQuit)
    }
}

@Composable
private fun GameOverOverlay(result: GameResult, highscore: Int, onRetry: () -> Unit, onMenu: () -> Unit) {
    Overlay {
        Text("Spiel vorbei", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
        if (result.isNewHighscore) {
            Spacer(Modifier.height(6.dp))
            Text("Neuer Rekord!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GameColors.Gold)
        }
        Spacer(Modifier.height(8.dp))
        Text("${result.score}", fontSize = 56.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text("Punkte", color = GameColors.TextMuted)
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Stat(label = "Treffer", value = result.hits)
            Stat(label = "Beste Serie", value = result.bestCombo)
            Stat(label = "Rekord", value = highscore)
        }
        Spacer(Modifier.height(28.dp))
        PrimaryButton(text = "Nochmal", onClick = onRetry)
        Spacer(Modifier.height(12.dp))
        SecondaryButton(text = "Zum Menü", onClick = onMenu)
    }
}

@Composable
private fun Overlay(content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelMedium, color = GameColors.TextMuted)
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
    ) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(26.dp),
    ) {
        Text(text, fontSize = 17.sp, color = Color.White)
    }
}
