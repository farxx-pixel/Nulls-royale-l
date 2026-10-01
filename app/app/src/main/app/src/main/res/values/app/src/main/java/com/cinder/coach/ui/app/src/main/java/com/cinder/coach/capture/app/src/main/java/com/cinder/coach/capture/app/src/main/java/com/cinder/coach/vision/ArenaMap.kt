// language: Kotlin, file: ArenaMap.kt
// fixed pixel regions for 1080x2400 portrait. Tune per device if needed.
package com.cinder.coach.vision

object ArenaMap {
    // elixir bar
    const val ELIXIR_X0 = 268
    const val ELIXIR_X1 = 1002
    const val ELIXIR_Y0 = 2340
    const val ELIXIR_Y1 = 2368

    // hand slots (centers)
    val HAND_SLOT_CENTERS = arrayOf(
        311 to 2214,
        471 to 2214,
        631 to 2214,
        791 to 2214
    )
    const val HAND_PATCH = 40

    // arena bounds
    const val ARENA_X0 = 90
    const val ARENA_X1 = 990
    const val ARENA_Y0 = 180
    const val ARENA_Y1 = 1960
    const val TILE_W = 50f
    const val TILE_H = 55.6f

    // HP bar regions
    val YOUR_LEFT_TOWER   = intArrayOf(145, 1405, 345, 1425)
    val YOUR_RIGHT_TOWER  = intArrayOf(735, 1405, 935, 1425)
    val ENEMY_LEFT_TOWER  = intArrayOf(145, 405, 345, 425)
    val ENEMY_RIGHT_TOWER = intArrayOf(735, 405, 935, 425)
    val ENEMY_KING        = intArrayOf(390, 370, 690, 390)
    val YOUR_KING         = intArrayOf(390, 1940, 690, 1960)

    fun pixelToTile(px: Int, py: Int): Pair<Int, Int> {
        val tx = ((px - ARENA_X0) / TILE_W).toInt().coerceIn(0, 17)
        val ty = ((py - ARENA_Y0) / TILE_H).toInt().coerceIn(0, 31)
        return tx to ty
    }
}
