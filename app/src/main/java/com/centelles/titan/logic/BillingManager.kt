package com.centelles.titan.logic

import android.content.Context
import android.widget.Toast

/**
 * Placeholder for Google Play Billing integration.
 */
class BillingManager(private val context: Context) {
    
    fun purchaseRemoveAds(onSuccess: () -> Unit) {
        // Simulated purchase
        Toast.makeText(context, "Purchasing 'Remove Ads'... (Simulated)", Toast.LENGTH_SHORT).show()
        onSuccess()
    }
}
