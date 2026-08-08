package com.centelles.titan.logic

import android.content.Context
import android.widget.Toast

/**
 * Placeholder for Google AdMob integration.
 */
class AdsManager(private val context: Context) {
    
    fun showRewardedAd(onEarnedReward: () -> Unit) {
        // In a real implementation, this would load and show an AdMob rewarded ad.
        // For now, we simulate with a Toast and immediate reward.
        Toast.makeText(context, "Watching Ad... (Simulated)", Toast.LENGTH_SHORT).show()
        onEarnedReward()
    }
    
    fun showInterstitialAd() {
        // Placeholder for interstitial ads (e.g., after Rebirth)
    }
}
