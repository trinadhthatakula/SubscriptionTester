package com.sub.tester.repo

import android.content.SharedPreferences
import androidx.core.content.edit
import com.android.billingclient.api.Purchase
import com.sub.tester.model.BillingHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SubscriptionManager(
    private val billingHelper: BillingHelper,
    private val prefs: SharedPreferences
) {
    // Scope for long-running observation
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Observe Active Subscriptions
        billingHelper.activeSubsList.onEach { purchases ->
            syncPurchasesToPrefs(purchases)
        }.launchIn(scope) // Keeps running as long as the app is alive

        // Observe One-Time Products
        billingHelper.oneTimeList.onEach { purchases ->
            syncPurchasesToPrefs(purchases)
        }.launchIn(scope)
    }

    private fun syncPurchasesToPrefs(purchases: List<Purchase>) {
        if (purchases.isEmpty()) return

        // ONE edit transaction for ALL updates. Much faster.
        prefs.edit {
            purchases.forEach { purchase ->
                // Check if purchased. Note: In V8, check purchaseState explicitly if needed
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    purchase.products.forEach { productId ->
                        putBoolean(productId, true)
                    }
                }
            }
        }
    }
}