package io.github.sembe123.punktjaeger.game

import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class TargetKind { NORMAL, GOLD, BOMB }

/** Ein Punkt auf dem Spielfeld. Alle Koordinaten und Radien sind in Pixeln. */
class Target(
    val id: Long,
    val kind: TargetKind,
    x: Float,
    y: Float,
    val maxRadius: Float,
    /** Lebensdauer in Sekunden. */
    val lifetime: Float,
    val colorIndex: Int,
) {
    var x: Float = x
        internal set
    var y: Float = y
        internal set
    var age: Float = 0f
        internal set

    /** 0 = gerade erschienen, 1 = abgelaufen. */
    val progress: Float get() = (age / lifetime).coerceIn(0f, 1f)

    /** Verbleibende Zeit als Anteil (1 = voll, 0 = abgelaufen). */
    val remaining: Float get() = 1f - progress

    /** Radius ohne Einblend-Animation – wird für die Trefferprüfung verwendet. */
    val coreRadius: Float get() = maxRadius * (1f - SHRINK * progress)

    /** Sichtbarer Radius inklusive kurzer Einblend-Animation. */
    val radius: Float
        get() {
            val t = (age / POP_IN_TIME).coerceIn(0f, 1f)
            val eased = 1f - (1f - t) * (1f - t) * (1f - t)
            return coreRadius * eased
        }

    val isExpired: Boolean get() = age >= lifetime

    companion object {
        const val POP_IN_TIME = 0.15f
        const val SHRINK = 0.5f
    }
}

enum class EffectKind { HIT, GOLD, BOMB, MISS, EMPTY_TAP, FADE }

/** Kurzlebiger visueller Effekt (Treffer, Explosion, ...). */
class Effect(
    val kind: EffectKind,
    val x: Float,
    val y: Float,
    val radius: Float,
    val colorIndex: Int = 0,
    val points: Int = 0,
    val healed: Boolean = false,
) {
    var age: Float = 0f
        internal set

    val duration: Float
        get() = when (kind) {
            EffectKind.BOMB -> 0.9f
            EffectKind.HIT, EffectKind.GOLD, EffectKind.MISS -> 0.75f
            EffectKind.EMPTY_TAP, EffectKind.FADE -> 0.35f
        }

    val progress: Float get() = (age / duration).coerceIn(0f, 1f)
    val isFinished: Boolean get() = age >= duration
}

/** Ereignisse, auf die die Oberfläche reagieren kann (z. B. mit Vibration). */
enum class GameEvent { HIT, GOLD, BOMB, MISS, GAME_OVER }

/**
 * Die komplette Spiellogik von Punktjäger – ohne Android-Abhängigkeiten,
 * damit sie sich einfach testen lässt.
 */
class GameEngine(private val random: Random = Random.Default) {

    private val _targets = mutableListOf<Target>()
    val targets: List<Target> get() = _targets

    private val _effects = mutableListOf<Effect>()
    val effects: List<Effect> get() = _effects

    private val events = mutableListOf<GameEvent>()

    var score = 0
        private set
    var lives = MAX_LIVES
        private set
    var combo = 0
        private set
    var bestCombo = 0
        private set
    var hits = 0
        private set
    var elapsed = 0f
        private set
    var isGameOver = false
        private set

    /** Verbleibende Zeit (Sekunden), in der das Spielfeld wackelt. */
    var shake = 0f
        private set

    var left = 0f
        private set
    var top = 0f
        private set
    var right = 0f
        private set
    var bottom = 0f
        private set
    var baseRadius = 60f
        private set
    var minTouchRadius = 48f
        private set

    private var spawnTimer = FIRST_SPAWN_DELAY
    private var nextId = 0L

    val hasBounds: Boolean get() = right > left && bottom > top

    /** Punkte-Multiplikator: steigt alle [COMBO_STEP] Treffer in Folge. */
    val multiplier: Int get() = min(1 + combo / COMBO_STEP, MAX_MULTIPLIER)

    /** Legt die Spielfläche fest und schiebt vorhandene Punkte notfalls hinein. */
    fun setBounds(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        baseRadius: Float,
        minTouchRadius: Float,
    ) {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
        this.baseRadius = baseRadius
        this.minTouchRadius = minTouchRadius
        for (t in _targets) {
            t.x = clampInto(t.x, left + t.maxRadius, right - t.maxRadius)
            t.y = clampInto(t.y, top + t.maxRadius, bottom - t.maxRadius)
        }
    }

    fun reset() {
        _targets.clear()
        _effects.clear()
        events.clear()
        score = 0
        lives = MAX_LIVES
        combo = 0
        bestCombo = 0
        hits = 0
        elapsed = 0f
        isGameOver = false
        shake = 0f
        spawnTimer = FIRST_SPAWN_DELAY
        nextId = 0L
    }

    /** Rückt das Spiel um [dt] Sekunden vor. */
    fun update(dt: Float) {
        if (dt <= 0f) return

        for (e in _effects) e.age += dt
        _effects.removeAll { it.isFinished }
        shake = max(0f, shake - dt)

        if (isGameOver || !hasBounds) return
        elapsed += dt

        for (t in _targets) t.age += dt
        val expired = _targets.filter { it.isExpired }
        if (expired.isNotEmpty()) {
            _targets.removeAll { it.isExpired }
            for (t in expired) {
                if (isGameOver) break
                onExpired(t)
            }
        }
        if (isGameOver) return

        spawnTimer -= dt
        while (spawnTimer <= 0f) {
            spawn()
            spawnTimer += spawnInterval()
        }
    }

    /**
     * Verarbeitet einen Fingertipp. Gibt `true` zurück, wenn ein Punkt getroffen wurde.
     * Daneben tippen beendet die aktuelle Serie.
     */
    fun tap(x: Float, y: Float): Boolean {
        if (isGameOver) return false

        var hit: Target? = null
        var bestRatio = Float.MAX_VALUE
        for (t in _targets) {
            val hitRadius = max(t.coreRadius, minTouchRadius) * HIT_SLOP
            val ratio = hypot(t.x - x, t.y - y) / hitRadius
            if (ratio <= 1f && ratio < bestRatio) {
                bestRatio = ratio
                hit = t
            }
        }

        if (hit == null) {
            combo = 0
            _effects += Effect(EffectKind.EMPTY_TAP, x, y, minTouchRadius)
            return false
        }

        _targets.remove(hit)
        when (hit.kind) {
            TargetKind.NORMAL -> {
                registerHit()
                val points = BASE_POINTS * multiplier
                score += points
                _effects += Effect(EffectKind.HIT, hit.x, hit.y, hit.coreRadius, hit.colorIndex, points)
                events += GameEvent.HIT
            }

            TargetKind.GOLD -> {
                registerHit()
                val points = GOLD_POINTS * multiplier
                score += points
                val healed = lives < MAX_LIVES
                if (healed) lives++
                _effects += Effect(EffectKind.GOLD, hit.x, hit.y, hit.coreRadius, hit.colorIndex, points, healed)
                events += GameEvent.GOLD
            }

            TargetKind.BOMB -> {
                combo = 0
                shake = BOMB_SHAKE
                _effects += Effect(EffectKind.BOMB, hit.x, hit.y, hit.coreRadius)
                events += GameEvent.BOMB
                loseLife()
            }
        }
        return true
    }

    /** Gibt alle seit dem letzten Aufruf aufgetretenen Ereignisse zurück. */
    fun drainEvents(): List<GameEvent> {
        if (events.isEmpty()) return emptyList()
        val copy = events.toList()
        events.clear()
        return copy
    }

    /** Abstand zwischen zwei neuen Punkten – wird mit der Zeit kürzer. */
    fun spawnInterval(): Float =
        MIN_SPAWN_INTERVAL + (START_SPAWN_INTERVAL - MIN_SPAWN_INTERVAL) * exp(-elapsed / SPAWN_DECAY)

    /** Lebensdauer eines normalen Punktes – wird mit der Zeit kürzer. */
    fun targetLifetime(): Float =
        MIN_LIFETIME + (START_LIFETIME - MIN_LIFETIME) * exp(-elapsed / LIFETIME_DECAY)

    fun bombChance(): Float =
        if (elapsed < BOMB_START) 0f
        else min(MAX_BOMB_CHANCE, 0.08f + (elapsed - BOMB_START) / 400f)

    /** Nur für Tests: legt einen Punkt an einer festen Stelle an. */
    internal fun addTarget(
        kind: TargetKind,
        x: Float,
        y: Float,
        radius: Float = baseRadius,
        lifetime: Float = 2f,
    ): Target {
        val target = Target(nextId++, kind, x, y, radius, lifetime, 0)
        _targets += target
        return target
    }

    private fun registerHit() {
        combo++
        hits++
        bestCombo = max(bestCombo, combo)
    }

    private fun onExpired(t: Target) {
        when (t.kind) {
            TargetKind.NORMAL -> {
                combo = 0
                shake = max(shake, MISS_SHAKE)
                _effects += Effect(EffectKind.MISS, t.x, t.y, t.coreRadius)
                events += GameEvent.MISS
                loseLife()
            }
            // Verpasstes Gold ist schade, aber keine Strafe; Bomben soll man ja liegen lassen.
            TargetKind.GOLD, TargetKind.BOMB ->
                _effects += Effect(EffectKind.FADE, t.x, t.y, t.coreRadius)
        }
    }

    private fun loseLife() {
        lives = max(0, lives - 1)
        if (lives == 0) gameOver()
    }

    private fun gameOver() {
        isGameOver = true
        for (t in _targets) _effects += Effect(EffectKind.FADE, t.x, t.y, t.coreRadius)
        _targets.clear()
        events += GameEvent.GAME_OVER
    }

    private fun spawn() {
        val roll = random.nextFloat()
        val bombChance = bombChance()
        val kind = when {
            roll < bombChance -> TargetKind.BOMB
            roll < bombChance + GOLD_CHANCE -> TargetKind.GOLD
            else -> TargetKind.NORMAL
        }
        val radius = when (kind) {
            TargetKind.NORMAL -> baseRadius * (0.85f + random.nextFloat() * 0.3f)
            TargetKind.GOLD -> baseRadius * 0.75f
            TargetKind.BOMB -> baseRadius * 0.95f
        }
        val lifetime = targetLifetime() * when (kind) {
            TargetKind.NORMAL -> 1f
            TargetKind.GOLD -> 0.65f
            TargetKind.BOMB -> 1.4f
        }
        val (x, y) = findFreeSpot(radius)
        _targets += Target(nextId++, kind, x, y, radius, lifetime, random.nextInt(PALETTE_SIZE))
    }

    /** Sucht eine Stelle, die möglichst weit von vorhandenen Punkten entfernt ist. */
    private fun findFreeSpot(radius: Float): Pair<Float, Float> {
        val minX = left + radius
        val maxX = right - radius
        val minY = top + radius
        val maxY = bottom - radius
        var bestX = (left + right) / 2f
        var bestY = (top + bottom) / 2f
        var bestGap = Float.NEGATIVE_INFINITY
        for (attempt in 0 until SPAWN_ATTEMPTS) {
            val x = if (maxX > minX) minX + random.nextFloat() * (maxX - minX) else (left + right) / 2f
            val y = if (maxY > minY) minY + random.nextFloat() * (maxY - minY) else (top + bottom) / 2f
            var gap = Float.POSITIVE_INFINITY
            for (t in _targets) {
                gap = min(gap, hypot(t.x - x, t.y - y) - t.maxRadius - radius)
            }
            if (gap > bestGap) {
                bestGap = gap
                bestX = x
                bestY = y
            }
            if (gap >= radius * 0.5f) break
        }
        return bestX to bestY
    }

    private fun clampInto(value: Float, lo: Float, hi: Float): Float =
        if (hi < lo) (lo + hi) / 2f else value.coerceIn(lo, hi)

    companion object {
        const val MAX_LIVES = 3
        const val COMBO_STEP = 10
        const val MAX_MULTIPLIER = 5
        const val BASE_POINTS = 10
        const val GOLD_POINTS = 50
        const val PALETTE_SIZE = 5

        const val BOMB_SHAKE = 0.4f
        const val MISS_SHAKE = 0.2f

        private const val HIT_SLOP = 1.15f
        private const val FIRST_SPAWN_DELAY = 0.6f
        private const val START_SPAWN_INTERVAL = 1.1f
        private const val MIN_SPAWN_INTERVAL = 0.26f
        private const val SPAWN_DECAY = 40f
        private const val START_LIFETIME = 2.6f
        private const val MIN_LIFETIME = 0.9f
        private const val LIFETIME_DECAY = 55f
        private const val BOMB_START = 10f
        private const val MAX_BOMB_CHANCE = 0.22f
        private const val GOLD_CHANCE = 0.05f
        private const val SPAWN_ATTEMPTS = 12
    }
}
