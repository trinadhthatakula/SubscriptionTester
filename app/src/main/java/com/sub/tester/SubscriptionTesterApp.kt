package com.sub.tester

import android.app.Application
import androidx.core.content.edit
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.sub.tester.model.BillingHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SubscriptionTesterApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val sp = getSharedPreferences("prefs", MODE_PRIVATE)

        val billingHelper = BillingHelper.getInstance(this)
        CoroutineScope(Dispatchers.IO).launch {
            billingHelper.fetchPurchasesAsync { result ->
                if (result.isSuccess) {
                    result.getOrDefault(emptyList()).forEach { purchase ->
                        purchase.products.forEach { product ->
                            sp.edit {
                                putBoolean(
                                    product,
                                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                                )
                            }
                        }
                    }
                }
            }
        }

    }

}