package com.sub.tester.model

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun BillingResult.getReadableResponse(): Pair<String, String> {
    // We don't need to log here since the caller usually logs the result
    return when (responseCode) {
        BillingResponseCode.OK -> "OK" to "All is Well"
        BillingResponseCode.BILLING_UNAVAILABLE -> "Billing Unavailable" to "Can't use billing right now. Please try again later."
        BillingResponseCode.ITEM_UNAVAILABLE -> "Item Unavailable" to "Item is not available."
        BillingResponseCode.ITEM_ALREADY_OWNED -> "Item Already Purchased" to "You have already purchased this item."
        BillingResponseCode.DEVELOPER_ERROR -> "Developer Error" to "Something went wrong (Developer Error)."
        BillingResponseCode.FEATURE_NOT_SUPPORTED -> "Feature Not Supported" to "This feature is not supported by your device."
        BillingResponseCode.NETWORK_ERROR -> "Network Error" to "Please check your internet connection."
        BillingResponseCode.USER_CANCELED -> "User Cancelled" to "You have cancelled the transaction."
        BillingResponseCode.SERVICE_UNAVAILABLE -> "Service Unavailable" to "Billing service not found. Please try again later."
        BillingResponseCode.SERVICE_DISCONNECTED -> "Service Disconnected" to "Billing service disconnected. Restart Play Store and try again."
        else -> "Unknown Error" to "Something went wrong. Code: $responseCode"
    }
}

// Move this to a configuration object or specialized provider
val productList = listOf(
    ProductData("premium_sub", BillingClient.ProductType.SUBS),
    ProductData("premium_user", BillingClient.ProductType.INAPP),
)

fun ProductData.toProductDetailsParams(): QueryProductDetailsParams.Product =
    QueryProductDetailsParams.Product.newBuilder()
        .setProductId(id)
        .setProductType(type)
        .build()

// Remove manual Singleton. Inject this via Koin as single { BillingHelper(androidContext()) }
class BillingHelper(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    var lastState = ""
    var stateObserver: ((String) -> Unit)? = null

    private var _mutableProductDetailsList = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetailsList = _mutableProductDetailsList.asStateFlow()

    private var _mutableActiveSubsList = MutableStateFlow<List<Purchase>>(emptyList())
    val activeSubsList = _mutableActiveSubsList.asStateFlow()

    private var _mutableOneTimeList = MutableStateFlow<List<Purchase>>(emptyList())
    val oneTimeList = _mutableOneTimeList.asStateFlow()

    // Callback for the specific purchase flow in progress
    private var purchaseListener: ((Result<String>) -> Unit)? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
            processPurchases(purchases)
        } else {
            // Handle user cancellation or errors
            val (response, msg) = billingResult.getReadableResponse()
            log("Purchase Update Error: $response - $msg")
            if(billingResult.responseCode != BillingResponseCode.USER_CANCELED) {
                purchaseListener?.invoke(Result.failure(Exception(msg)))
            }
        }
    }

    private var billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection() // NEW IN V8: Handles retry logic for you
        .build()

    init {
        startConnection()
    }

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                // V8 Auto-reconnect handles the retry, but we update UI state
                updateState("Billing service disconnected")
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    updateState("Billing service Connected")
                    // Fetch everything once connected
                    refreshAllData()
                } else {
                    updateState(billingResult.getReadableResponse().second)
                }
            }
        })
    }

    fun refreshAllData() {
        scope.launch {
            fetchPurchases()
            fetchAvailableProducts()
        }
    }

    /**
     * In V8, queryPurchasesAsync replaces the old history/query methods for active items.
     * Note: queryPurchaseHistory is REMOVED in v8. Do not try to use it.
     */
    suspend fun fetchPurchases() {
        updateState("Fetching purchases...")

        // 1. Fetch Subscriptions
        val subsResult = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        if (subsResult.billingResult.responseCode == BillingResponseCode.OK) {
            _mutableActiveSubsList.update { subsResult.purchasesList }
        }

        // 2. Fetch In-App Products
        val inAppResult = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        if (inAppResult.billingResult.responseCode == BillingResponseCode.OK) {
            _mutableOneTimeList.update { inAppResult.purchasesList }
        }

        updateState("Purchases updated")
    }

    suspend fun fetchAvailableProducts() {
        updateState("Fetching product details...")

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList.map { it.toProductDetailsParams() })
            .build()

        val result = billingClient.queryProductDetails(params)

        if (result.billingResult.responseCode == BillingResponseCode.OK) {
            _mutableProductDetailsList.update { result.productDetailsList ?: emptyList() }
            updateState("Product details fetched")
        } else {
            updateState("Error fetching details: ${result.billingResult.debugMessage}")
        }
    }

    /**
     * Dynamic Offer Token Logic
     * Never hardcode "TRIAL". Always find a valid offer.
     */
    fun launchPurchaseFlow(
        activity: Activity,
        productDetails: ProductDetails,
        pListener: ((Result<String>) -> Unit)? = null
    ) {
        purchaseListener = pListener

        val productDetailsParamsList = if (productDetails.productType == BillingClient.ProductType.SUBS) {
            // Logic: Pick the first available offer, or a specific base plan if you have logic for it.
            // This example picks the first offer token available.
            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken

            if (offerToken == null) {
                pListener?.invoke(Result.failure(Exception("No valid offer found for subscription")))
                return
            }

            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )
        } else {
            // One-time purchase (INAPP) does not use offer tokens
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .build()
            )
        }

        val billingResult = billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()
        )

        if (billingResult.responseCode != BillingResponseCode.OK) {
            pListener?.invoke(Result.failure(Exception(billingResult.debugMessage)))
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        CoroutineScope(Dispatchers.IO).launch {
            // Acknowledge logic goes here (omitted for brevity, but MANDATORY)
            // You must verify signature and call acknowledgePurchase or consumeAsync

            withContext(Dispatchers.Main) {
                purchaseListener?.invoke(Result.success("Purchase Successful"))
                refreshAllData() // Refresh lists after purchase
            }
        }
    }

    private fun updateState(msg: String) {
        lastState = msg
        stateObserver?.invoke(msg)
        log("State: $msg")
    }

    private fun log(msg: String) {
        Log.d("BillingHelper", msg)
    }
}