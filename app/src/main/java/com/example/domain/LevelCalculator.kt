package com.example.domain

data class LevelInfo(
    val level: Int,
    val currentLevelXp: Int,
    val requiredXpForNextLevel: Int,
    val totalXp: Int,
    val progress: Float
)

object LevelCalculator {

    fun calculateLevelInfo(totalXp: Int): LevelInfo {
        val safeTotalXp = totalXp.coerceAtLeast(0)
        var currentXp = safeTotalXp
        var level = 1
        var req = xpForLevel(level)
        while (currentXp >= req) {
            currentXp -= req
            level++
            req = xpForLevel(level)
        }
        val progress = if (req > 0) (currentXp.toFloat() / req.toFloat()).coerceIn(0f, 1f) else 0f
        return LevelInfo(
            level = level,
            currentLevelXp = currentXp,
            requiredXpForNextLevel = req,
            totalXp = safeTotalXp,
            progress = progress
        )
    }

    fun xpForLevel(level: Int): Int {
        return 100 + ((level - 1) * 40)
    }
}
