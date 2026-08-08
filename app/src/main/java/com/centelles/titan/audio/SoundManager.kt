package com.centelles.titan.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundManager(private val context: Context) {
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(5)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    // Map to store sound IDs (TODO: Load these from res/raw)
    private val sounds = mutableMapOf<String, Int>()

    fun playTitanHit() {
        // TODO: soundPool.play(sounds["titan_hit"] ?: return, 1f, 1f, 0, 0, 1f)
    }

    fun playCrackHit() {
        // TODO: soundPool.play(sounds["crack_hit"] ?: return, 1f, 1f, 0, 0, 1f)
    }

    fun playShardCollect() {
        // TODO: soundPool.play(sounds["shard_collect"] ?: return, 0.5f, 0.5f, 0, 0, 1f)
    }

    fun playRecruitSprite() {
        // TODO: soundPool.play(sounds["recruit"] ?: return, 1f, 1f, 0, 0, 1f)
    }

    fun playUpgradePurchase() {
        // TODO: soundPool.play(sounds["upgrade"] ?: return, 1f, 1f, 0, 0, 1f)
    }

    fun playRebirth() {
        // TODO: soundPool.play(sounds["rebirth"] ?: return, 1f, 1f, 0, 0, 1f)
    }

    fun startAmbientMusic() {
        // TODO: Use MediaPlayer or ExoPlayer for looping music
    }

    fun stopAmbientMusic() {
        // TODO: Stop looping music
    }
    
    fun release() {
        soundPool.release()
    }
}
