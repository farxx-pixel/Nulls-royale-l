// language: Kotlin, file: FrameAnalyzer.kt
// fixed-region reads -> MatchState
package com.cinder.coach.vision

import android.graphics.Bitmap
import android.graphics.Color
import com.cinder.coach.logic.MatchState

class FrameAnalyzer(private val screenW: Int, private val screenH: Int) {

    private val sx = screenW / 1080f
    private val sy = screenH / 2400f

    fun analyze(bmp: Bitmap): MatchState {
        val elixir = readElixir(bmp)
        val hand = readHand(bmp)
        val towers = readTowers(bmp)
        return MatchState(elixir, hand, towers, -1)
    }

    private fun readElixir(bmp: Bitmap): Int {
        val x0 = (ArenaMap.ELIXIR_X0 * sx).toInt().coerceIn(0, bmp.width - 1)
        val x1 = (ArenaMap.ELIXIR_X1 * sx).toInt().coerceIn(0, bmp.width - 1)
        val y  = ((ArenaMap.ELIXIR_Y0 + ArenaMap.ELIXIR_Y1) / 2 * sy).toInt()
            .coerceIn(0, bmp.height - 1)

        var lastFilled = x0
        var x = x0
        while (x < x1) {
            if (isMagenta(bmp.getPixel(x, y))) lastFilled = x else break
            x += 4
        }
        val frac = (lastFilled - x0).toFloat() / (x1 - x0).coerceAtLeast(1)
        return (frac * 10f).toInt().coerceIn(0, 10)
    }

    private fun isMagenta(c: Int): Boolean {
        val r = Color.red(c); val g = Color.green(c); val b = Color.blue(c)
        return r > 160 && b > 160 && g < 150
    }

    private fun readHand(bmp: Bitmap): List<String> {
        val out = ArrayList<String>(4)
        for ((cx0, cy0) in ArenaMap.HAND_SLOT_CENTERS) {
            val cx = (cx0 * sx).toInt().coerceIn(0, bmp.width - 1)
            val cy = (cy0 * sy).toInt().coerceIn(0, bmp.height - 1)
            out.add(CardTemplates.identify(bmp, cx, cy))
        }
        return out
    }

    data class TowerHp(
        val yourLeftFrac: Float, val yourRightFrac: Float, val yourKingFrac: Float,
        val enemyLeftFrac: Float, val enemyRightFrac: Float, val enemyKingFrac: Float
    )

    private fun readTowers(bmp: Bitmap): TowerHp = TowerHp(
        readBarFill(bmp, ArenaMap.YOUR_LEFT_TOWER, true),
        readBarFill(bmp, ArenaMap.YOUR_RIGHT_TOWER, true),
        readBarFill(bmp, ArenaMap.YOUR_KING, true),
        readBarFill(bmp, ArenaMap.ENEMY_LEFT_TOWER, false),
        readBarFill(bmp, ArenaMap.ENEMY_RIGHT_TOWER, false),
        readBarFill(bmp, ArenaMap.ENEMY_KING, false)
    )

    private fun readBarFill(bmp: Bitmap, box: IntArray, yourBar: Boolean): Float {
        val x0 = (box[0] * sx).toInt().coerceIn(0, bmp.width - 1)
        val y0 = (box[1] * sy).toInt().coerceIn(0, bmp.height - 1)
        val x1 = (box[2] * sx).toInt().coerceIn(0, bmp.width - 1)
        val y1 = (box[3] * sy).toInt().coerceIn(0, bmp.height - 1)
        val y = ((y0 + y1) / 2).coerceIn(0, bmp.height - 1)
        val total = (x1 - x0).coerceAtLeast(1)

        var filled = 0
        for (x in x0 until x1) {
            val c = bmp.getPixel(x, y)
            if (if (yourBar) isBlueHp(c) else isRedHp(c)) filled = x - x0 + 1
        }
        if (filled < 8) return 0f
        return (filled.toFloat() / total).coerceIn(0f, 1f)
    }

    private fun isBlueHp(c: Int): Boolean {
        val r = Color.red(c); val g = Color.green(c); val b = Color.blue(c)
        return b > 180 && b - r > 60 && g in 100..200
    }
    private fun isRedHp(c: Int): Boolean {
        val r = Color.red(c); val g = Color.green(c); val b = Color.blue(c)
        return r > 180 && r - g > 80 && r - b > 80
    }
}
