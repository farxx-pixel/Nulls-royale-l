// language: Kotlin, file: Advisor.kt
// v1 rule-based advisor
package com.cinder.coach.logic

data class Suggestion(
    val card: String,
    val side: String,
    val reason: String
)

object Advisor {

    private val cost = mapOf(
        "knight" to 3, "archer" to 3, "giant" to 5, "goblin" to 2,
        "minion" to 3, "skeleton_army" to 4, "fireball" to 4, "log" to 2,
        "hog_rider" to 4, "musketeer" to 4, "valkyrie" to 4, "baby_dragon" to 4,
        "mega_knight" to 7, "pekka" to 7, "inferno_tower" to 5, "tesla" to 4,
        "cannon" to 3, "mini_pekka" to 4, "arrows" to 3, "zap" to 2,
        "prince" to 5, "witch" to 5, "balloon" to 5, "electro_wizard" to 4
    )

    private val counters = mapOf(
        "hog_rider"   to listOf("cannon", "tesla", "skeleton_army", "mini_pekka"),
        "mega_knight" to listOf("inferno_tower", "skeleton_army", "pekka"),
        "balloon"     to listOf("tesla", "inferno_tower", "musketeer"),
        "giant"       to listOf("inferno_tower", "mini_pekka", "skeleton_army"),
        "pekka"       to listOf("skeleton_army", "inferno_tower", "witch"),
        "witch"       to listOf("fireball", "valkyrie", "log"),
        "prince"      to listOf("skeleton_army", "mini_pekka", "tesla"),
        "goblin"      to listOf("log", "arrows", "zap")
    )

    private val winConditions = setOf(
        "hog_rider", "giant", "balloon", "mega_knight", "pekka", "prince"
    )

    fun decide(state: MatchState): Suggestion {
        if (state.elixir >= 10) {
            val cheapest = state.hand.minByOrNull { cost[it] ?: 4 } ?: "unknown"
            return Suggestion(cheapest, "LEFT", "elixir full — cycle")
        }

        val myHp = state.myTowerHp()
        val theirHp = state.enemyTowerHp()

        if (theirHp < myHp - 0.15f && state.elixir >= 6) {
            val push = bestPushCard(state.hand)
                ?: return Suggestion("", "WAIT", "no push card")
            val side = if (state.towers.enemyLeftFrac < state.towers.enemyRightFrac)
                "LEFT" else "RIGHT"
            return Suggestion(push, side, "push weak side")
        }

        if (myHp < theirHp - 0.15f) {
            return Suggestion("", "WAIT", "save elixir, defend")
        }

        val cheapest = state.hand.minByOrNull { cost[it] ?: 4 } ?: "unknown"
        return Suggestion(cheapest, "LEFT", "neutral cycle")
    }

    private fun bestPushCard(hand: List<String>): String? =
        hand.firstOrNull { it in winConditions }
            ?: hand.maxByOrNull { cost[it] ?: 4 }

    fun counterPlay(hand: List<String>, enemyCards: List<String>, elixir: Int): Suggestion? {
        for (e in enemyCards) {
            val answers = counters[e] ?: continue
            for (a in answers) if (a in hand && (cost[a] ?: 4) <= elixir) {
                return Suggestion(a, "CENTER", "counter $e")
            }
        }
        return null
    }
}
