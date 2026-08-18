package com.centelles.titan.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object TitanAnalytics {
    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    }

    fun logLayerReached(layer: Int, stage: Int) {
        val bundle = Bundle().apply {
            putInt("layer_index", layer)
            putInt("at_stage", stage)
        }
        firebaseAnalytics?.logEvent("layer_reached", bundle)
    }

    fun logDescent(stage: Int, starlightEarned: Double, layer: Int) {
        val bundle = Bundle().apply {
            putInt("stage_reached", stage)
            putDouble("starlight_earned", starlightEarned)
            putInt("current_layer", layer)
        }
        firebaseAnalytics?.logEvent("descent_performed", bundle)
    }

    fun logUpgradeBought(upgradeId: String, level: Int) {
        val bundle = Bundle().apply {
            putString("upgrade_id", upgradeId)
            putInt("new_level", level)
        }
        firebaseAnalytics?.logEvent("upgrade_bought", bundle)
    }
}
