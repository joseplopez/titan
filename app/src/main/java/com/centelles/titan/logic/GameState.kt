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
data class LayerDefinition(
    val level: Int,
    val name: String,
    val flavor: String,
    val hpMultiplier: Double,
    val mechanicalTwist: String = "",
    val finalStage: Int = 10 // Every layer has 10 stages for now
)

val LAYERS = listOf(
    LayerDefinition(
        level = 1,
        name = "The Amber Shell",
        flavor = "Sun-warmed outer stone, the part the world above has always known.",
        hpMultiplier = 1.0
    ),
    LayerDefinition(
        level = 2,
        name = "The Frozen Marrow",
        flavor = "Colder, older — the Titan's dreams turn to ice here.",
        hpMultiplier = 5.0,
        mechanicalTwist = "brittle_resistance"
    ),
    LayerDefinition(
        level = 3,
        name = "The Ember Core",
        flavor = "Something is still burning at its center, after all this time.",
        hpMultiplier = 25.0,
        mechanicalTwist = "hp_regen"
    ),
    LayerDefinition(
        level = 4,
        name = "The Verdant Hollow",
        flavor = "Root-systems from the world above have grown all the way down, undisturbed for millennia.",
        hpMultiplier = 125.0
    )
)

@Serializable
data class Talent(
    val id: String,
    val name: String,
    val description: String,
    val tree: TalentTree,
    val baseCost: Double,
    val costMultiplier: Double,
    val prerequisites: Map<String, Int> = emptyMap()
)

enum class TalentTree { MIGHT, CRAFT, WILD }

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
    val currentLayer: Int = 1,
    val deepestLayerReached: Int = 1,
    val hasSeenLayerIntro: Map<Int, Boolean> = emptyMap(),
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
    val currentLayerDef: LayerDefinition get() = LAYERS.find { it.level == currentLayer } ?: LAYERS.first()

    // Basic stats with Permanent Talents applied
    private val starlightDpsMult: Double get() = 1.0 + permanentTalents.getOrDefault("starlight_dps", 0) * 0.2
    private val starlightCpsMult: Double get() = 1.0 + permanentTalents.getOrDefault("starlight_cps", 0) * 0.2
    private val starlightCritMult: Double get() = 1.0 + permanentTalents.getOrDefault("starlight_crit", 0) * 0.1
    private val starlightElementMult: Double get() = 1.0 + permanentTalents.getOrDefault("starlight_elements", 0) * 0.15
    
    // Temporary Boost
    private val boostMultiplier: Double get() = if (System.currentTimeMillis() < boostEndTime) 2.0 else 1.0

    val clickDamage: Double get() {
        // Base damage scales with upgrades (increased multiplier from 1.1 to 1.2)
        val upgradeBase = 1.0 * (1.2.pow(upgrades.getOrDefault("click_power", 0)))
        // Scaling with total DPS (5% of total DPS is added to click power)
        val dpsScaling = totalDps * 0.05
        
        val base = (upgradeBase + dpsScaling) * starlightDpsMult * boostMultiplier
        
        // Mechanical Twist: Brittle resistance in Layer 2
        val penalty = if (currentLayerDef.mechanicalTwist == "brittle_resistance" && frostSprites == 0) 0.2 else 1.0
        
        return (base * penalty).coerceAtLeast(if (penalty < 1.0) 0.1 else 1.0)
    }

    val crackDamageMult: Double get() = 5.0 * starlightCritMult

    val strikerDps: Double get() = 1.0 * (1.1.pow(upgrades.getOrDefault("sprite_efficiency", 0))) * starlightDpsMult * boostMultiplier
    val gathererCps: Double get() = 1.0 * (1.1.pow(upgrades.getOrDefault("sprite_efficiency", 0))) * starlightCpsMult * boostMultiplier

    // Elemental effects
    val emberDot: Double get() = emberSprites * 0.5 * (1.1.pow(upgrades.getOrDefault("sprite_efficiency", 0))) * starlightDpsMult * starlightElementMult
    val frostMultiplier: Double get() = 1.0 + (frostSprites * 0.05 * starlightElementMult) // +5% damage per Frost sprite
    val thornCpsBonus: Double get() = thornSprites * 2.0 * starlightElementMult // Thorn sprites are better at collecting
    
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
        val talent = TALENTS.find { it.id == id } ?: return 10.0
        val level = permanentTalents.getOrDefault(id, 0)
        return talent.baseCost * talent.costMultiplier.pow(level)
    }

    fun canUnlockTalent(id: String): Boolean {
        val talent = TALENTS.find { it.id == id } ?: return false
        
        // Tree unlock requirements
        when (talent.tree) {
            TalentTree.CRAFT -> if (deepestLayerReached < 2) return false
            TalentTree.WILD -> if (deepestLayerReached < 3) return false
            else -> {}
        }

        // Prerequisite requirements
        for ((prereqId, requiredLevel) in talent.prerequisites) {
            if (permanentTalents.getOrDefault(prereqId, 0) < requiredLevel) {
                return false
            }
        }
        
        return true
    }

    companion object {
        val TALENTS = listOf(
            Talent("starlight_dps", "Celestial Might", "+20% DPS", TalentTree.MIGHT, 1.0, 2.0),
            Talent("starlight_crit", "Stellar Focus", "+10% Crack Damage", TalentTree.MIGHT, 1.0, 2.0, mapOf("starlight_dps" to 1)),
            
            Talent("starlight_cps", "Astral Greed", "+20% CPS", TalentTree.CRAFT, 1.0, 2.0),
            Talent("starlight_costs", "Spirit Call", "-5% Sprite Costs", TalentTree.CRAFT, 3.0, 3.0, mapOf("starlight_cps" to 1)),
            
            Talent("starlight_capacity", "Expansive Groves", "+5 Sprite Capacity", TalentTree.WILD, 2.0, 2.5),
            Talent("starlight_elements", "Primordial Aura", "+15% Elemental Potency", TalentTree.WILD, 5.0, 3.0, mapOf("starlight_capacity" to 1))
        )
    }

    fun calculateStarlightReward(): Double {
        // Updated formula: scales with current layer depth
        if (currentRunShardsEarned < 1000) return 0.0
        val base = (currentRunShardsEarned / 10000.0).pow(0.5)
        val layerBonus = currentLayer.toDouble().pow(1.5)
        return base * (awakeningStage + 1) * layerBonus
    }

    fun isUpgradeUnlocked(id: String): Boolean {
        return upgrades.containsKey(id)
    }

    fun canDescend(): Boolean {
        return awakeningStage >= currentLayerDef.finalStage
    }
}
