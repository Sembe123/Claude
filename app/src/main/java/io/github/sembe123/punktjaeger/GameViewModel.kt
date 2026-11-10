package io.github.sembe123.punktjaeger

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import io.github.sembe123.punktjaeger.game.GameEngine
import io.github.sembe123.punktjaeger.game.GameEvent

enum class Screen { MENU, PLAYING, GAME_OVER }

data class GameResult(
    val score: Int,
    val hits: Int,
    val bestCombo: Int,
    val isNewHighscore: Boolean,
)

/** Hält das laufende Spiel, damit es z. B. beim Drehen des Bildschirms erhalten bleibt. */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val engine = GameEngine()

    var screen by mutableStateOf(Screen.MENU)
        private set
    var paused by mutableStateOf(false)
        private set

    /** Wird jedes Bild erhöht; das Spielfeld zeichnet sich dadurch neu. */
    var animationTime by mutableFloatStateOf(0f)
        private set

    var score by mutableIntStateOf(0)
        private set
    var lives by mutableIntStateOf(GameEngine.MAX_LIVES)
        private set
    var combo by mutableIntStateOf(0)
        private set
    var multiplier by mutableIntStateOf(1)
        private set
    var highscore by mutableIntStateOf(prefs.getInt(KEY_HIGHSCORE, 0))
        private set
    var result by mutableStateOf<GameResult?>(null)
        private set

    fun startGame() {
        engine.reset()
        paused = false
        result = null
        syncHud()
        screen = Screen.PLAYING
    }

    /** Ein Bild der Spielschleife. Gibt die dabei aufgetretenen Ereignisse zurück. */
    fun step(dt: Float): List<GameEvent> {
        if (screen == Screen.MENU || paused) return emptyList()
        // Nach Spielende nur so lange weiterzeichnen, bis die letzten Effekte verklungen sind.
        if (screen == Screen.GAME_OVER && engine.effects.isEmpty()) return emptyList()

        engine.update(dt)
        animationTime += dt
        val events = engine.drainEvents()
        syncHud()
        if (screen == Screen.PLAYING && engine.isGameOver) finishGame()
        return events
    }

    fun tap(x: Float, y: Float) {
        if (screen != Screen.PLAYING || paused) return
        engine.tap(x, y)
        syncHud()
    }

    fun pause() {
        if (screen == Screen.PLAYING && !engine.isGameOver) paused = true
    }

    fun resume() {
        paused = false
    }

    fun backToMenu() {
        paused = false
        screen = Screen.MENU
    }

    private fun syncHud() {
        score = engine.score
        lives = engine.lives
        combo = engine.combo
        multiplier = engine.multiplier
    }

    private fun finishGame() {
        val newHighscore = engine.score > highscore
        if (newHighscore) {
            highscore = engine.score
            prefs.edit().putInt(KEY_HIGHSCORE, highscore).apply()
        }
        result = GameResult(engine.score, engine.hits, engine.bestCombo, newHighscore)
        screen = Screen.GAME_OVER
    }

    private companion object {
        const val PREFS_NAME = "punktjaeger"
        const val KEY_HIGHSCORE = "highscore"
    }
}
