package io.github.sembe123.punktjaeger.game

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameEngineTest {

    private lateinit var engine: GameEngine

    @Before
    fun setUp() {
        engine = GameEngine(Random(42))
        engine.setBounds(0f, 0f, 1000f, 1800f, baseRadius = 80f, minTouchRadius = 60f)
    }

    @Test
    fun tappingNormalTargetScoresAndStartsCombo() {
        engine.addTarget(TargetKind.NORMAL, 500f, 500f)

        assertTrue(engine.tap(510f, 495f))

        assertEquals(GameEngine.BASE_POINTS, engine.score)
        assertEquals(1, engine.combo)
        assertEquals(1, engine.hits)
        assertTrue(engine.targets.isEmpty())
        assertEquals(listOf(GameEvent.HIT), engine.drainEvents())
    }

    @Test
    fun tappingNextToATargetMissesAndResetsCombo() {
        engine.addTarget(TargetKind.NORMAL, 200f, 200f)
        engine.tap(200f, 200f)
        engine.addTarget(TargetKind.NORMAL, 500f, 500f)

        assertFalse(engine.tap(900f, 1500f))

        assertEquals(0, engine.combo)
        assertEquals(1, engine.targets.size)
        assertEquals(GameEngine.MAX_LIVES, engine.lives)
    }

    @Test
    fun expiredNormalTargetCostsALife() {
        engine.addTarget(TargetKind.NORMAL, 500f, 500f, lifetime = 0.1f)

        engine.update(0.2f)

        assertEquals(GameEngine.MAX_LIVES - 1, engine.lives)
        assertTrue(GameEvent.MISS in engine.drainEvents())
        assertFalse(engine.isGameOver)
    }

    @Test
    fun losingAllLivesEndsTheGame() {
        repeat(GameEngine.MAX_LIVES + 1) { i ->
            engine.addTarget(TargetKind.NORMAL, 150f + i * 200f, 500f, lifetime = 0.1f)
        }

        engine.update(0.2f)

        assertTrue(engine.isGameOver)
        assertEquals(0, engine.lives)
        assertTrue(engine.targets.isEmpty())
        assertTrue(GameEvent.GAME_OVER in engine.drainEvents())
        // Nach Spielende passiert nichts mehr.
        assertFalse(engine.tap(500f, 500f))
        engine.update(5f)
        assertTrue(engine.targets.isEmpty())
    }

    @Test
    fun tappingABombCostsALifeAndResetsCombo() {
        engine.addTarget(TargetKind.NORMAL, 200f, 200f)
        engine.tap(200f, 200f)
        engine.addTarget(TargetKind.BOMB, 600f, 600f)

        assertTrue(engine.tap(600f, 600f))

        assertEquals(GameEngine.MAX_LIVES - 1, engine.lives)
        assertEquals(0, engine.combo)
        assertTrue(engine.shake > 0f)
        assertTrue(GameEvent.BOMB in engine.drainEvents())
    }

    @Test
    fun ignoredBombIsHarmless() {
        engine.addTarget(TargetKind.BOMB, 600f, 600f, lifetime = 0.1f)

        engine.update(0.2f)

        assertEquals(GameEngine.MAX_LIVES, engine.lives)
        assertTrue(engine.drainEvents().isEmpty())
    }

    @Test
    fun goldGivesBonusAndHealsALife() {
        engine.addTarget(TargetKind.BOMB, 600f, 600f)
        engine.tap(600f, 600f)
        engine.addTarget(TargetKind.GOLD, 300f, 300f)

        engine.tap(300f, 300f)

        assertEquals(GameEngine.GOLD_POINTS, engine.score)
        assertEquals(GameEngine.MAX_LIVES, engine.lives)
    }

    @Test
    fun goldNeverExceedsMaxLives() {
        engine.addTarget(TargetKind.GOLD, 300f, 300f)

        engine.tap(300f, 300f)

        assertEquals(GameEngine.MAX_LIVES, engine.lives)
    }

    @Test
    fun multiplierGrowsWithComboAndIsCapped() {
        repeat(GameEngine.COMBO_STEP) {
            engine.addTarget(TargetKind.NORMAL, 500f, 500f)
            engine.tap(500f, 500f)
        }
        assertEquals(2, engine.multiplier)
        // 9 Treffer à 10 Punkte + 1 Treffer mit Multiplikator 2
        assertEquals(9 * 10 + 20, engine.score)

        repeat(GameEngine.COMBO_STEP * 10) {
            engine.addTarget(TargetKind.NORMAL, 500f, 500f)
            engine.tap(500f, 500f)
        }
        assertEquals(GameEngine.MAX_MULTIPLIER, engine.multiplier)
        assertEquals(GameEngine.COMBO_STEP * 11, engine.bestCombo)
    }

    @Test
    fun overlappingTargetsPreferTheClosestCenter() {
        val far = engine.addTarget(TargetKind.NORMAL, 500f, 500f)
        val near = engine.addTarget(TargetKind.BOMB, 560f, 500f)

        engine.tap(555f, 500f)

        assertTrue(far in engine.targets)
        assertFalse(near in engine.targets)
    }

    @Test
    fun gameGetsHarderOverTime() {
        val startInterval = engine.spawnInterval()
        val startLifetime = engine.targetLifetime()
        assertEquals(0f, engine.bombChance(), 0f)

        // Ohne Treffer würde das Spiel schnell enden, deshalb werden alle normalen Punkte angetippt.
        repeat(120 * 60) {
            engine.update(1f / 60f)
            engine.targets.filter { it.kind != TargetKind.BOMB }.forEach { engine.tap(it.x, it.y) }
        }

        assertFalse(engine.isGameOver)
        assertTrue(engine.spawnInterval() < startInterval)
        assertTrue(engine.targetLifetime() < startLifetime)
        assertTrue(engine.spawnInterval() >= 0.26f)
        assertTrue(engine.bombChance() > 0f)
    }

    @Test
    fun spawnedTargetsStayInsideBounds() {
        engine.setBounds(40f, 200f, 1040f, 2000f, baseRadius = 80f, minTouchRadius = 60f)
        repeat(60 * 60) {
            engine.update(1f / 60f)
            for (t in engine.targets) {
                assertTrue(t.x - t.maxRadius >= 40f - 0.01f && t.x + t.maxRadius <= 1040f + 0.01f)
                assertTrue(t.y - t.maxRadius >= 200f - 0.01f && t.y + t.maxRadius <= 2000f + 0.01f)
            }
            if (engine.isGameOver) engine.reset()
        }
    }

    @Test
    fun shrinkingBoundsMovesTargetsInside() {
        val t = engine.addTarget(TargetKind.NORMAL, 900f, 1700f)

        engine.setBounds(0f, 0f, 500f, 800f, baseRadius = 80f, minTouchRadius = 60f)

        assertTrue(t.x + t.maxRadius <= 500f)
        assertTrue(t.y + t.maxRadius <= 800f)
    }

    @Test
    fun randomPlayNeverCrashes() {
        val random = Random(7)
        repeat(20) {
            engine.reset()
            var steps = 0
            while (!engine.isGameOver && steps < 60 * 300) {
                engine.update(random.nextFloat() * 0.05f)
                if (random.nextFloat() < 0.2f) engine.tap(random.nextFloat() * 1000f, random.nextFloat() * 1800f)
                engine.drainEvents()
                steps++
            }
            assertTrue(engine.score >= 0)
            assertTrue(engine.lives in 0..GameEngine.MAX_LIVES)
        }
    }

    @Test
    fun resetStartsAFreshGame() {
        engine.addTarget(TargetKind.NORMAL, 500f, 500f)
        engine.tap(500f, 500f)
        engine.update(3f)

        engine.reset()

        assertEquals(0, engine.score)
        assertEquals(GameEngine.MAX_LIVES, engine.lives)
        assertEquals(0, engine.combo)
        assertTrue(engine.targets.isEmpty())
        assertFalse(engine.isGameOver)
    }
}
