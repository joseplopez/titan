package com.centelles.titan.logic

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.centelles.titan.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Manages Google AdMob rewarded ads.
 */
class AdsManager(private val context: Context) {
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    private val REWARDED_AD_UNIT_ID = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/5224354917" // Test ID
    } else {
        "ca-app-pub-9749336798654274/4897755376" // Production ID
    }

    init {
        MobileAds.initialize(context) {
            loadRewardedAd()
        }
    }

    private fun loadRewardedAd() {
        if (isLoading || rewardedAd != null) return
        isLoading = true

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, REWARDED_AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("AdsManager", "Ad failed to load: ${adError.message}")
                rewardedAd = null
                isLoading = false
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d("AdsManager", "Ad was loaded.")
                rewardedAd = ad
                isLoading = false
            }
        })
    }

    fun isAdAvailable(): Boolean = rewardedAd != null

    fun showRewardedAd(activity: Activity, onEarnedReward: () -> Unit) {
        val ad = rewardedAd
        if (ad != null) {
            ad.show(activity) { rewardItem ->
                Log.d("AdsManager", "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                onEarnedReward()
            }
            rewardedAd = null
            loadRewardedAd()
        } else {
            Toast.makeText(context, "No ad available — try again soon", Toast.LENGTH_SHORT).show()
            loadRewardedAd()
        }
    }
}
