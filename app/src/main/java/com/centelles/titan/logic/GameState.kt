package com.centelles.titan.logic

import kotlinx.serialization.Serializable
import kotlin.math.pow
import kotlin.math.round

@Serializable
data class Crack(
    val id: String,
    val x: Float, // Normalized 0.0 - 1.0
    val y: Float, // Normalized 0.0 - 1.0
    val expiryTime: Long
)

@Serializable
data class GameState(
    val shardsBanked: Double = 0.0,
    val shardsOnGround: Double = 0.0,
    val strikersCount: Int = 0,
    val gatherersCount: Int = 0,
    val emberSprites: Int = 0,
    val frostSprites: Int = 0,
    val thornSprites: Int = 0,
    val grovesCount: Int = 1,
    val titanHp: Double = 100.0,
    val maxTitanHp: Double = 100.0,
    val awakeningStage: Int = 0,
    val totalLifetimeShards: Double = 0.0,
    val currentRunShardsEarned: Double = 0.0,
    val starlight: Double = 0.0,
    val permanentTalents: Map<String, Int> = emptyMap(),
    val upgrades: Map<String, Int> = emptyMap(),
    val activeCracks: List<Crack> = emptyList(),
    val boostEndTime: Long = 0,
    val adsRemoved: Boolean = false,
    val isTutorialCompleted: Boolean = false,
    val hasSeenIntro: Boolean = false
) {
    // Basic stats with Permanent Talents applied
    private val starlightDpsMult: Double get() = 1.0 + permanentTalents.getOrDefault("starlight_dps", 0) * 0.2
    private val starlightCpsMult: Double get() = 1.0 + permanentTalents.getOrDefault("starlight_cps", 0) * 0.2
    
    // Temporary Boost
    private val boostMultiplier: Double get() = if (System.currentTimeMillis() < boostEndTime) 2.0 else 1.0

    val clickDamage: Double get() = 1.0 * (1.1.pow(upgrades.getOrDefault("click_power", 0))) * starlightDpsMult * boostMultiplier
    val strikerDps: Double get() = 1.0 * (1.1.pow(upgrades.getOrDefault("sprite_efficiency", 0))) * starlightDpsMult * boostMultiplier
    val gathererCps: Double get() = 1.0 * (1.1.pow(upgrades.getOrDefault("sprite_efficiency", 0))) * starlightCpsMult * boostMultiplier

    // Elemental effects
    val emberDot: Double get() = emberSprites * 0.5 * (1.1.pow(upgrades.getOrDefault("sprite_efficiency", 0))) * starlightDpsMult
    val frostMultiplier: Double get() = 1.0 + (frostSprites * 0.05) // +5% damage per Frost sprite
    val thornCpsBonus: Double get() = thornSprites * 2.0 // Thorn sprites are better at collecting
    
    val totalDps: Double get() = (strikersCount * strikerDps + emberDot) * frostMultiplier
    val totalCps: Double get() = (gatherersCount * gathererCps) + (thornSprites * (gathererCps + thornCpsBonus))
    
    val spriteCapacity: Int get() = (grovesCount * 10) + permanentTalents.getOrDefault("starlight_capacity", 0) * 5
    val currentSpriteCount: Int get() = strikersCount + gatherersCount + emberSprites + frostSprites + thornSprites

    // Cost scaling with Permanent Talent applied (Cheaper Sprites)
    private val costReduction: Double get() = 0.95.pow(permanentTalents.getOrDefault("starlight_costs", 0))

    fun getStrikerCost(): Double = 10.0 * 1.15.pow(strikersCount) * costReduction
    fun getGathererCost(): Double = 10.0 * 1.15.pow(gatherersCount) * costReduction
    fun getEmberCost(): Double = 50.0 * 1.2.pow(emberSprites) * costReduction
    fun getFrostCost(): Double = 75.0 * 1.25.pow(frostSprites) * costReduction
    fun getThornCost(): Double = 40.0 * 1.18.pow(thornSprites) * costReduction
    fun getGroveCost(): Double = 100.0 * 2.0.pow(grovesCount - 1)
    
    fun getUpgradeCost(id: String): Double {
        val level = upgrades.getOrDefault(id, 0)
        return when (id) {
            "click_power" -> 25.0 * 1.5.pow(level)
            "sprite_efficiency" -> 50.0 * 1.6.pow(level)
            "unlock_ember" -> 500.0
            "unlock_frost" -> 1000.0
            "unlock_thorn" -> 300.0
            else -> 1000.0
        }
    }

    fun getTalentCost(id: String): Double {
        val level = permanentTalents.getOrDefault(id, 0)
        return when (id) {
            "starlight_dps" -> 1.0 * 2.0.pow(level)
            "starlight_cps" -> 1.0 * 2.0.pow(level)
            "starlight_capacity" -> 2.0 * 2.5.pow(level)
            "starlight_costs" -> 3.0 * 3.0.pow(level)
            else -> 10.0
        }
    }

    fun calculateStarlightReward(): Double {
        // Simple scaling: log-based or fraction of total run shards
        // Award roughly 1 starlight for every 10k shards in the first awakening
        return if (currentRunShardsEarned < 1000) 0.0 else (currentRunShardsEarned / 10000.0).pow(0.5) * (awakeningStage + 1)
    }

    fun isUpgradeUnlocked(id: String): Boolean {
        return upgrades.containsKey(id)
    }
}
