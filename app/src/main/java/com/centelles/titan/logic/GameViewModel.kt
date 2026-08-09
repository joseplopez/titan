package com.centelles.titan.logic

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
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GameRepository(application)
    private val adsManager = AdsManager(application)
    private val billingManager = BillingManager(application)
    
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
            repository.gameStateFlow.collect { savedState ->
                if (savedState != null) {
                    _state.value = savedState
                }
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

            // 3. Strikers deal damage (Staggered shots from all strikers)
            var damageDealt = 0.0
            val maxVisualStrikers = 10
            val strikerCount = min(current.strikersCount, maxVisualStrikers)
            
            if (strikerCount > 0) {
                val shotInterval = 2000 / strikerCount
                if (currentTime - lastStrikerAttackTime >= shotInterval) {
                    lastStrikerAttackTime = currentTime
                    // Cycle through strikers
                    val strikerIndex = ( (currentTime / shotInterval) % strikerCount).toInt()
                    
                    // Total damage for the time elapsed since last check (0.1s tick)
                    // But we only fire when interval passes. To stay accurate:
                    val damagePerShot = current.totalDps * (shotInterval / 1000.0)
                    
                    _events.tryEmit(GameEvent.StrikerHit(damagePerShot, strikerIndex))
                    damageDealt = damagePerShot
                }
            }
            
            // 4. Gatherers collect shards
            val shardsToCollect = min(current.shardsOnGround, current.totalCps * deltaTime)
            if (shardsToCollect > 0) {
                _events.tryEmit(GameEvent.ShardsCollected(shardsToCollect))
            }
            
            val newHp = max(0.0, current.titanHp - damageDealt)
            
            val updated = current.copy(
                shardsBanked = current.shardsBanked + shardsToCollect,
                shardsOnGround = max(0.0, current.shardsOnGround + damageDealt - shardsToCollect),
                titanHp = newHp,
                activeCracks = newCracks,
                currentRunShardsEarned = current.currentRunShardsEarned + damageDealt,
                totalLifetimeShards = current.totalLifetimeShards + damageDealt
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
        val nextMaxHp = 100.0 * 2.5.pow(nextStage)
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
            
            val damage = current.clickDamage * 5.0
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

    fun onRebirth() {
        _state.update { current ->
            val reward = current.calculateStarlightReward()
            if (current.awakeningStage >= 1 || current.starlight > 0) {
                GameState(
                    starlight = current.starlight + reward,
                    permanentTalents = current.permanentTalents,
                    totalLifetimeShards = current.totalLifetimeShards,
                    adsRemoved = current.adsRemoved,
                    isTutorialCompleted = current.isTutorialCompleted,
                    hasSeenIntro = current.hasSeenIntro
                )
            } else {
                current
            }
        }
        viewModelScope.launch {
            repository.saveGameState(_state.value)
        }
    }

    fun buyTalent(id: String) {
        _state.update { current ->
            val cost = current.getTalentCost(id)
            if (current.starlight >= cost) {
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

    fun watchAdForBoost() {
        adsManager.showRewardedAd {
            _state.update { it.copy(boostEndTime = System.currentTimeMillis() + 60000) } // 1 minute boost
        }
    }

    fun purchaseRemoveAds() {
        billingManager.purchaseRemoveAds {
            _state.update { it.copy(adsRemoved = true) }
        }
    }

    fun completeTutorial() {
        _state.update { it.copy(isTutorialCompleted = true) }
    }

    fun setHasSeenIntro() {
        _state.update { it.copy(hasSeenIntro = true) }
    }
}
