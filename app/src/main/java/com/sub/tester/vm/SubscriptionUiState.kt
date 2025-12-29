package com.sub.tester.vm

import com.android.billingclient.api.ProductDetails

data class SubscriptionUiState(
    val isLoading: Boolean = true,
    val subscriptions: List<ProductDetails> = emptyList(),
    val oneTimeProducts: List<ProductDetails> = emptyList(),
    val userMessage: String? = null // For errors or success toasts
)