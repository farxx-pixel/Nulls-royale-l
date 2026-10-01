// language: Kotlin, file: CardTemplates.kt
// identifies a hand slot by matching against PNG templates in assets/cards/
package com.cinder.coach.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.InputStream

object CardTemplates {

    private val signatures = mutableMapOf<String, IntArray>()
    @Volatile private var loaded = false

    fun init(ctx: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val assets = ctx.assets
            val names = runCatching { assets.list("cards") }.getOrNull() ?: emptyArray()
            for (f in names) {
                if (!f.endsWith(".png")) continue
                runCatching {
                    assets.open("cards/$f").use { ins: InputStream ->
                        val bmp = BitmapFactory.decodeStream(ins) ?: return@use
                        signatures[f.removeSuffix(".png")] = samplePatch(bmp, bmp.width / 2, bmp.height / 2)
                    }
                }
            }
            loaded = true
        }
    }

    fun identify(bmp: Bitmap, cx: Int, cy: Int): String {
        if (signatures.isEmpty()) return "unknown"
        val patch = samplePatch(bmp, cx, cy)
        var best = "unknown"
        var bestDist = Int.MAX_VALUE
        for ((name, sig) in signatures) {
            val d = l1(patch, sig)
            if (d < bestDist) { bestDist = d; best = name }
        }
        return if (bestDist < 30_000) best else "unknown"
    }

    private fun samplePatch(bmp: Bitmap, cx: Int, cy: Int): IntArray {
        val out = IntArray(75)
        var i = 0
        val half = 20
        for (gy in 0 until 5) {
            for (gx in 0 until 5) {
                val x = (cx - half + gx * (2 * half / 4)).coerceIn(0, bmp.width - 1)
                val y = (cy - half + gy * (2 * half / 4)).coerceIn(0, bmp.height - 1)
                var r = 0; var g = 0; var b = 0; var n = 0
                for (dx in -3..3) for (dy in -3..3) {
                    val px = bmp.getPixel(
                        (x + dx).coerceIn(0, bmp.width - 1),
                        (y + dy).coerceIn(0, bmp.height - 1)
                    )
                    r += (px shr 16) and 0xFF
                    g += (px shr 8) and 0xFF
                    b += px and 0xFF
                    n++
                }
                out[i++] = r / n; out[i++] = g / n; out[i++] = b / n
            }
        }
        return out
    }

    private fun l1(a: IntArray, b: IntArray): Int {
        var s = 0
        val n = minOf(a.size, b.size)
        for (i in 0 until n) s += kotlin.math.abs(a[i] - b[i])
        return s
    }
}
