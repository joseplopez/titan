package com.centelles.titan.logic

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.centelles.titan.data.GameRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

sealed class GameEvent {
    data class StrikerHit(val damage: Double, val strikerIndex: Int) : GameEvent()
    data class ShardsCollected(val amount: Double) : GameEvent()
    data class LayerStoryReveal(val layer: Int) : GameEvent()
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GameRepository(application)
    private val adsManager = AdsManager(application)
    
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private var lastStrikerAttackTime = 0L

    init {
        loadState()
        startGameLoop()
        startSaveLoop()
    }

    private fun loadState() {
        viewModelScope.launch {
            // Only load the initial state from disk once at startup
            val savedState = repository.gameStateFlow.firstOrNull()
            if (savedState != null) {
                _state.value = savedState
            }
        }
    }

    fun checkAndTriggerLayerIntro() {
        val current = _state.value
        if (current.hasSeenIntro && current.hasSeenLayerIntro.getOrDefault(current.currentLayer, false) == false) {
            viewModelScope.launch {
                _events.emit(GameEvent.LayerStoryReveal(current.currentLayer))
            }
        }
    }

    private fun startGameLoop() {
        viewModelScope.launch {
            while (true) {
                delay(100) // 10 updates per second
                tick(0.1)
            }
        }
    }

    private fun startSaveLoop() {
        viewModelScope.launch {
            while (true) {
                delay(5000) // Save every 5 seconds
                repository.saveGameState(_state.value)
            }
        }
    }

    private fun tick(deltaTime: Double) {
        val currentTime = System.currentTimeMillis()
        
        _state.update { current ->
            // 1. Clean up expired cracks
            val activeCracks = current.activeCracks.filter { it.expiryTime > currentTime }
            
            // 2. Potentially spawn a new crack
            val newCracks = if (activeCracks.size < 3 && Random.nextFloat() < 0.05) {
                val x = Random.nextFloat() * 0.6f + 0.2f 
                val y = Random.nextFloat() * 0.6f + 0.2f
                activeCracks + Crack(
                    id = UUID.randomUUID().toString(),
                    x = x,
                    y = y,
                    expiryTime = currentTime + 3000
                )
            } else {
                activeCracks
            }

            // 3. Strikers deal damage (Continuous volley)
            var totalDamageThisTick = 0.0
            val strikerCount = min(current.strikersCount, 10)
            if (strikerCount > 0) {
                // Fire one striker every (2000 / count) ms
                val interval = 2000 / strikerCount
                if (currentTime - lastStrikerAttackTime >= interval) {
                    lastStrikerAttackTime = currentTime
                    // Cycle through strikers
                    val strikerIndex = ( (currentTime / interval) % strikerCount).toInt()
                    
                    val damagePerShot = current.totalDps * (interval / 1000.0)
                    
                    // Apply mechanical twist: Brittle resistance in Layer 2
                    val finalDamage = if (current.currentLayerDef.mechanicalTwist == "brittle_resistance" && current.frostSprites == 0) {
                        (damagePerShot * 0.2).coerceAtLeast(1.0)
                    } else {
                        damagePerShot.coerceAtLeast(1.0)
                    }

                    _events.tryEmit(GameEvent.StrikerHit(finalDamage, strikerIndex))
                    totalDamageThisTick = finalDamage
                }
            }
            
            // 4. Gatherers collect shards
            val shardsToCollect = min(current.shardsOnGround, current.totalCps * deltaTime)
            if (shardsToCollect > 0) {
                _events.tryEmit(GameEvent.ShardsCollected(shardsToCollect))
            }
            
            // 5. HP Regen twist in Layer 3
            var regeneratedHp = 0.0
            if (current.currentLayerDef.mechanicalTwist == "hp_regen") {
                // Use a much more lenient scaling formula to prevent "unbeatable" walls
                // Percent-based regen reduced to 0.01% to keep it challenging but fair
                val totalRegenRate = (current.maxTitanHp * 0.0001) + (10.0 * (current.awakeningStage + 1))
                
                if (current.totalDps < totalRegenRate * 1.5) { // Very low threshold to stop regen
                   regeneratedHp = totalRegenRate * deltaTime * 10
                }
            }

            val newHp = min(current.maxTitanHp, max(0.0, current.titanHp - totalDamageThisTick + regeneratedHp))
            
            val updated = current.copy(
                shardsBanked = current.shardsBanked + shardsToCollect,
                shardsOnGround = max(0.0, current.shardsOnGround + totalDamageThisTick - shardsToCollect),
                titanHp = newHp,
                activeCracks = newCracks,
                currentRunShardsEarned = current.currentRunShardsEarned + totalDamageThisTick,
                totalLifetimeShards = current.totalLifetimeShards + totalDamageThisTick
            )
            
            if (newHp <= 0.0) {
                updated.handleAwakening()
            } else {
                updated
            }
        }
    }

    private fun GameState.handleAwakening(): GameState {
        val nextStage = awakeningStage + 1
        // HP scaling includes layer multiplier
        val nextMaxHp = 100.0 * 2.5.pow(nextStage) * currentLayerDef.hpMultiplier
        return copy(
            awakeningStage = nextStage,
            titanHp = nextMaxHp,
            maxTitanHp = nextMaxHp,
            activeCracks = emptyList()
        )
    }

    fun onTitanTap() {
        _state.update { current ->
            val damage = current.clickDamage
            val newHp = max(0.0, current.titanHp - damage)
            val updated = current.copy(
                shardsOnGround = current.shardsOnGround + damage,
                titanHp = newHp,
                currentRunShardsEarned = current.currentRunShardsEarned + damage,
                totalLifetimeShards = current.totalLifetimeShards + damage
            )
            if (newHp <= 0.0) {
                updated.handleAwakening()
            } else {
                updated
            }
        }
    }

    fun onCrackTap(id: String) {
        _state.update { current ->
            val crackIndex = current.activeCracks.indexOfFirst { it.id == id }
            if (crackIndex == -1) return@update current
            
            val damage = current.clickDamage * current.crackDamageMult
            val newHp = max(0.0, current.titanHp - damage)
            
            val updated = current.copy(
                shardsOnGround = current.shardsOnGround + damage,
                titanHp = newHp,
                activeCracks = current.activeCracks.filter { it.id != id },
                currentRunShardsEarned = current.currentRunShardsEarned + damage,
                totalLifetimeShards = current.totalLifetimeShards + damage
            )
            
            if (newHp <= 0.0) {
                updated.handleAwakening()
            } else {
                updated
            }
        }
    }

    fun onManualCollect() {
        _state.update { current ->
            current.copy(
                shardsBanked = current.shardsBanked + current.shardsOnGround,
                shardsOnGround = 0.0
            )
        }
    }

    fun recruitStriker() {
        _state.update { current ->
            val cost = current.getStrikerCost()
            if (current.shardsBanked >= cost && current.currentSpriteCount < current.spriteCapacity) {
                current.copy(
                    shardsBanked = current.shardsBanked - cost,
                    strikersCount = current.strikersCount + 1
                )
            } else {
                current
            }
        }
    }

    fun recruitGatherer() {
        _state.update { current ->
            val cost = current.getGathererCost()
            if (current.shardsBanked >= cost && current.currentSpriteCount < current.spriteCapacity) {
                current.copy(
                    shardsBanked = current.shardsBanked - cost,
                    gatherersCount = current.gatherersCount + 1
                )
            } else {
                current
            }
        }
    }

    fun recruitEmber() {
        _state.update { current ->
            val cost = current.getEmberCost()
            if (current.shardsBanked >= cost && current.currentSpriteCount < current.spriteCapacity) {
                current.copy(
                    shardsBanked = current.shardsBanked - cost,
                    emberSprites = current.emberSprites + 1
                )
            } else {
                current
            }
        }
    }

    fun recruitFrost() {
        _state.update { current ->
            val cost = current.getFrostCost()
            if (current.shardsBanked >= cost && current.currentSpriteCount < current.spriteCapacity) {
                current.copy(
                    shardsBanked = current.shardsBanked - cost,
                    frostSprites = current.frostSprites + 1
                )
            } else {
                current
            }
        }
    }

    fun recruitThorn() {
        _state.update { current ->
            val cost = current.getThornCost()
            if (current.shardsBanked >= cost && current.currentSpriteCount < current.spriteCapacity) {
                current.copy(
                    shardsBanked = current.shardsBanked - cost,
                    thornSprites = current.thornSprites + 1
                )
            } else {
                current
            }
        }
    }

    fun buildGrove() {
        _state.update { current ->
            val cost = current.getGroveCost()
            if (current.shardsBanked >= cost) {
                current.copy(
                    shardsBanked = current.shardsBanked - cost,
                    grovesCount = current.grovesCount + 1
                )
            } else {
                current
            }
        }
    }

    fun buyUpgrade(id: String) {
        _state.update { current ->
            val cost = current.getUpgradeCost(id)
            if (current.shardsBanked >= cost) {
                val newLevel = current.upgrades.getOrDefault(id, 0) + 1
                val newUpgrades = current.upgrades.toMutableMap()
                newUpgrades[id] = newLevel
                current.copy(
                    shardsBanked = current.shardsBanked - cost,
                    upgrades = newUpgrades
                )
            } else {
                current
            }
        }
    }

    fun onDescend(starlightBoosted: Boolean = false) {
        _state.update { current ->
            if (current.canDescend()) {
                var reward = current.calculateStarlightReward()
                if (starlightBoosted) {
                    reward *= 1.5
                }
                
                val nextLayer = current.currentLayer + 1
                val deepest = max(current.deepestLayerReached, nextLayer)
                
                // Reset run-specific state
                GameState(
                    starlight = current.starlight + reward,
                    permanentTalents = current.permanentTalents,
                    totalLifetimeShards = current.totalLifetimeShards,
                    currentLayer = nextLayer,
                    deepestLayerReached = deepest,
                    hasSeenLayerIntro = current.hasSeenLayerIntro,
                    isTutorialCompleted = current.isTutorialCompleted,
                    hasSeenIntro = current.hasSeenIntro,
                    lastAdWatchTimes = current.lastAdWatchTimes,
                    isFirstDescendCompleted = true
                ).let { newState ->
                    // Initialize Titan HP for the new layer
                    val maxHp = 100.0 * newState.currentLayerDef.hpMultiplier
                    newState.copy(
                        titanHp = maxHp,
                        maxTitanHp = maxHp
                    )
                }
            } else {
                current
            }
        }
        
        // Trigger story reveal for the new layer
        viewModelScope.launch {
            _events.emit(GameEvent.LayerStoryReveal(_state.value.currentLayer))
        }

        viewModelScope.launch {
            repository.saveGameState(_state.value)
        }
    }

    fun markLayerIntroSeen(layer: Int) {
        _state.update { current ->
            val newSeen = current.hasSeenLayerIntro.toMutableMap()
            newSeen[layer] = true
            current.copy(hasSeenLayerIntro = newSeen)
        }
    }

    fun buyTalent(id: String) {
        _state.update { current ->
            val cost = current.getTalentCost(id)
            if (current.starlight >= cost && current.canUnlockTalent(id)) {
                val newLevel = current.permanentTalents.getOrDefault(id, 0) + 1
                val newTalents = current.permanentTalents.toMutableMap()
                newTalents[id] = newLevel
                current.copy(
                    starlight = current.starlight - cost,
                    permanentTalents = newTalents
                )
            } else {
                current
            }
        }
    }

    fun watchAdForBoost(activity: Activity) {
        adsManager.showRewardedAd(activity) {
            _state.update {
                it.copy(
                    boostEndTime = System.currentTimeMillis() + 180000, // 3 minute boost
                    lastAdWatchTimes = it.lastAdWatchTimes + (GameState.AD_MULTIPLIER to System.currentTimeMillis())
                )
            }
        }
    }

    fun watchAdForShards(activity: Activity) {
        adsManager.showRewardedAd(activity) {
            _state.update { current ->
                val reward = (current.totalCps * 100.0).coerceAtLeast(10.0)
                current.copy(
                    shardsBanked = current.shardsBanked + reward,
                    lastAdWatchTimes = current.lastAdWatchTimes + (GameState.AD_SHARDS to System.currentTimeMillis())
                )
            }
        }
    }

    fun watchAdForFreeUpgrade(activity: Activity, upgradeId: String) {
        adsManager.showRewardedAd(activity) {
            _state.update { current ->
                if (upgradeId == "grove") {
                    current.copy(
                        grovesCount = current.grovesCount + 1,
                        lastAdWatchTimes = current.lastAdWatchTimes + (GameState.AD_FREE_UPGRADE to System.currentTimeMillis())
                    )
                } else {
                    val newLevel = current.upgrades.getOrDefault(upgradeId, 0) + 1
                    val newUpgrades = current.upgrades.toMutableMap()
                    newUpgrades[upgradeId] = newLevel
                    current.copy(
                        upgrades = newUpgrades,
                        lastAdWatchTimes = current.lastAdWatchTimes + (GameState.AD_FREE_UPGRADE to System.currentTimeMillis())
                    )
                }
            }
        }
    }

    fun watchAdForFreeTalent(activity: Activity, talentId: String) {
        adsManager.showRewardedAd(activity) {
            _state.update { current ->
                val newLevel = current.permanentTalents.getOrDefault(talentId, 0) + 1
                val newTalents = current.permanentTalents.toMutableMap()
                newTalents[talentId] = newLevel
                current.copy(
                    permanentTalents = newTalents,
                    lastAdWatchTimes = current.lastAdWatchTimes + (GameState.AD_FREE_TALENT to System.currentTimeMillis())
                )
            }
        }
    }

    fun watchAdForStarlightBoost(activity: Activity, onComplete: () -> Unit) {
        adsManager.showRewardedAd(activity) {
            _state.update { current ->
                current.copy(
                    lastAdWatchTimes = current.lastAdWatchTimes + (GameState.AD_STARLIGHT_BOOST to System.currentTimeMillis())
                )
            }
            onComplete()
        }
    }

    fun completeTutorial() {
        _state.update { it.copy(isTutorialCompleted = true) }
    }

    fun setHasSeenIntro() {
        _state.update { it.copy(hasSeenIntro = true) }
    }
}
