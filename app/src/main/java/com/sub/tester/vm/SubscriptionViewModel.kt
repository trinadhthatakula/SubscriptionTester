package com.sub.tester.vm

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.sub.tester.model.BillingHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class SubscriptionViewModel(private val billingHelper: BillingHelper) : ViewModel() {

    // Internal state for messages (errors/success)
    private val _userMessage = MutableStateFlow<String?>(null)

    // Combine BillingHelper data with our local state into one UiState
    val uiState: StateFlow<SubscriptionUiState> = combine(
        billingHelper.productDetailsList,
        _userMessage
    ) { productList, message ->

        val subs = productList.filter { it.productType == BillingClient.ProductType.SUBS }
        val inApp = productList.filter { it.productType == BillingClient.ProductType.INAPP }

        SubscriptionUiState(
            // Logic: If list is empty, we are loading.
            // In a real app, you'd add a specific 'isFetching' flag in BillingHelper.
            isLoading = productList.isEmpty() && message == null,
            subscriptions = subs,
            oneTimeProducts = inApp,
            userMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SubscriptionUiState()
    )

    fun buyProduct(activity: Activity, productDetails: ProductDetails) {
        billingHelper.launchPurchaseFlow(activity, productDetails) { result ->
            if (result.isFailure) {
                _userMessage.update { "Purchase Failed: ${result.exceptionOrNull()?.message}" }
            } else {
                _userMessage.update { "Purchase Successful!" }
            }
        }
    }

    // Call this from UI after showing the Snack/Toast to clear it
    fun messageShown() {
        _userMessage.update { null }
    }
}