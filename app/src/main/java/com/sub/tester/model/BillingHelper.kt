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
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchaseHistory
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

var productList = listOf(
    ProductData("premium_sub", BillingClient.ProductType.SUBS),
    ProductData("premium_user", BillingClient.ProductType.INAPP),
)

fun ProductData.toProductDetailsParams(): QueryProductDetailsParams.Product =
    QueryProductDetailsParams.Product.newBuilder()
        .setProductId(id)
        .setProductType(type)
        .build()

class BillingHelper(context: Context) {

    companion object{
        var INSTANCE: BillingHelper? = null

        fun getInstance(context: Context) = INSTANCE?:synchronized(this) {
            var instance = BillingHelper(context)
            INSTANCE = instance
            instance
        }
    }

    var lastState = ""
    var stateObserver: ((String) -> Unit)? = null

    private var mutableProductDetailsList : MutableStateFlow<List<ProductDetails>> = MutableStateFlow(emptyList())
    val productDetailsList : StateFlow<List<ProductDetails>> = mutableProductDetailsList

    private var mutableActiveSubsList : MutableStateFlow<List<Purchase>> = MutableStateFlow(emptyList())
    val activeSubsList : StateFlow<List<Purchase>> = mutableActiveSubsList

    private var mutableOneTimeList: MutableStateFlow<List<Purchase>> = MutableStateFlow(emptyList())
    val oneTimeList : StateFlow<List<Purchase>> = mutableOneTimeList

    private var purchaseListener: ((Result<String>) -> Unit)? = null
    private val defOfferToken = "TRIAL"

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
                purchases.forEach { purchase ->

                }
            } else {
                billingResult.getReadableResponse(true)
            }
        }

    private var billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    init {
        startConnection {
            CoroutineScope(Dispatchers.IO).launch {
                fetchPurchasesAsync{ result ->
                    if(result.isSuccess){
                        mutableActiveSubsList.update { result.getOrDefault(emptyList()) }
                    }else {
                        result.exceptionOrNull()?.printStackTrace()
                        lastState = result.exceptionOrNull()?.message ?: "Error fetching subscriptions"
                        stateObserver?.invoke(lastState)
                    }
                }
                fetchPurchasesAsync(BillingClient.ProductType.INAPP){ result ->
                    if(result.isSuccess){
                        mutableOneTimeList.update { result.getOrDefault(emptyList()) }
                    }else {
                        result.exceptionOrNull()?.printStackTrace()
                        lastState = result.exceptionOrNull()?.message ?: "Error fetching purchases"
                        stateObserver?.invoke(lastState)
                    }
                }
                fetchProductDetails(productList.filter { it.type == BillingClient.ProductType.SUBS }) { result ->
                    if (result.isSuccess) {
                        mutableProductDetailsList.update {
                            val mutableIt = it.toMutableList()
                            mutableIt.addAll(result.getOrDefault(emptyList()))
                            mutableIt
                        }
                    } else {
                        result.exceptionOrNull()?.printStackTrace()
                        lastState = result.exceptionOrNull()?.message ?: "Error fetching product details"
                        stateObserver?.invoke(lastState)
                    }
                }
                fetchProductDetails(productList.filter { it.type == BillingClient.ProductType.INAPP }) { result ->
                    if (result.isSuccess) {
                        mutableProductDetailsList.update {
                            val mutableIt = it.toMutableList()
                            mutableIt.addAll(result.getOrDefault(emptyList()))
                            mutableIt
                        }
                    } else {
                        result.exceptionOrNull()?.printStackTrace()
                        lastState = result.exceptionOrNull()?.message ?: "Error fetching product details"
                        stateObserver?.invoke(lastState)
                    }
                }
            }
        }
    }

    suspend fun fetchPurchasesAsync(
        productType: String = BillingClient.ProductType.SUBS,
        onResult: ((Result<List<Purchase>>) -> Unit)? = null
    ) {
        lastState = "Fetching purchases"
        stateObserver?.invoke(lastState)
        withContext(Dispatchers.IO) {
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(productType)
                    .build()
            )
        }.let { result ->
            if (result.billingResult.responseCode == BillingResponseCode.OK) {
                onResult?.invoke(Result.success(
                    result.purchasesList
                ))
            }else {
                onResult?.invoke(Result.failure(Exception("Error fetching purchases")))
                lastState = result.billingResult.getReadableResponse(false).second
                stateObserver?.invoke(lastState)
            }

        }
    }

    suspend fun fetchPurchaseHistory(
        productType: String = BillingClient.ProductType.SUBS,
        onResult: ((Result<List<PurchaseHistoryRecord>>) -> Unit)? = null
    ) {
        withContext(Dispatchers.IO) {
            billingClient.queryPurchaseHistory(
                QueryPurchaseHistoryParams.newBuilder()
                    .setProductType(productType)
                    .build()
            )
        }.let { result ->
            if (result.billingResult.responseCode == BillingResponseCode.OK) {
                onResult?.invoke(Result.success(result.purchaseHistoryRecordList ?: emptyList()))
            } else {
                onResult?.invoke(Result.failure(Exception("Error fetching purchases")))
            }
        }
    }

    fun launchPurchaseFlow(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String = defOfferToken,
        pListener: ((Result<String>) -> Unit)? = null
    ): BillingResult {
        purchaseListener = pListener
        return billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder().setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .apply {
                            setProductDetails(productDetails)
                            if (productDetails.productType == BillingClient.ProductType.SUBS)
                                setOfferToken(offerToken)
                        }.build()
                )
            ).build()
        )
    }

    suspend fun fetchProductDetails(
        productList: List<ProductData>,
        onResult: ((Result<List<ProductDetails>>) -> Unit)? = null
    ) {
        lastState = "Fetching product details"
        stateObserver?.invoke(lastState)
        withContext(Dispatchers.IO) {
            billingClient.queryProductDetails(
                QueryProductDetailsParams.newBuilder().apply {
                    setProductList(productList.map { it.toProductDetailsParams() })
                }.build()
            )
        }.let { productDetailsResult ->
            val billingResult = productDetailsResult.billingResult
            val productDetailsList = productDetailsResult.productDetailsList
            if (billingResult.responseCode == BillingResponseCode.OK && productDetailsList != null) {
                onResult?.invoke(Result.success(productDetailsList))
                lastState = "Fetched product details"
                stateObserver?.invoke(lastState)
            } else {
                onResult?.invoke(Result.failure(Exception("Error fetching product details")))
                lastState = productDetailsResult.billingResult.getReadableResponse(false).second
                stateObserver?.invoke(lastState)
            }
        }
    }

    var connected: Int = BillingClient.ConnectionState.CLOSED

    fun startConnection(onConnected: (() -> Unit)? = null) {
        if (billingClient.connectionState == BillingClient.ConnectionState.CONNECTED) {
            stateObserver?.invoke("Billing service connected")
            onConnected?.invoke()
        } else {
            billingClient.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingServiceDisconnected() {
                        connected = BillingClient.ConnectionState.DISCONNECTED
                        lastState = "Billing service disconnected"
                        stateObserver?.invoke(lastState)
                    }

                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (billingResult.responseCode == BillingResponseCode.OK) {
                            // The BillingClient is ready. You can query purchases here.
                            connected = BillingClient.ConnectionState.CONNECTED
                            lastState = ("Billing service Connected")
                            stateObserver?.invoke(lastState)
                            onConnected?.invoke()
                        }else{
                            lastState = billingResult.getReadableResponse(false).second
                            stateObserver?.invoke(lastState)
                        }
                    }

                }
            )
        }
    }


}

fun BillingResult.getReadableResponse(
    shouldLog: Boolean = false
): Pair<String, String> {
    if (shouldLog) {
        Log.d(
            "BillingHelper",
            "handleBillingResponse: ${responseCode}, debug message: $debugMessage"
        )
    }
    return when (responseCode) {
        BillingResponseCode.OK -> {
            Pair("OK", "All is Well")
        }

        BillingResponseCode.BILLING_UNAVAILABLE -> {
            Pair("Billing Unavailable", "Can't use billing right now Please try again later")
        }

        BillingResponseCode.ITEM_UNAVAILABLE -> {
            Pair("Item Unavailable", "Item is not available")
        }

        BillingResponseCode.ITEM_ALREADY_OWNED -> {
            Pair("Item Already Purchased", "You have already purchased this Item")
        }

        BillingResponseCode.DEVELOPER_ERROR -> {
            Pair("Developer Error", "Something went wrong")
        }

        BillingResponseCode.FEATURE_NOT_SUPPORTED -> {
            Pair("Feature Not Supported", "This feature is not supported by your device")
        }

        BillingResponseCode.NETWORK_ERROR -> {
            Pair("Network Error", "Please check your internet connection")
        }

        BillingResponseCode.USER_CANCELED -> {
            Pair("User Cancelled the transaction", "You have cancelled the transaction")
        }

        BillingResponseCode.SERVICE_UNAVAILABLE -> {
            Pair("Billing service not found", "Please try again later")
        }

        BillingResponseCode.SERVICE_DISCONNECTED -> {
            Pair("Billing service disconnected", "Restart play store and try again later")
        }

        else -> {
            Pair("Developer Error", "Something went wrong")
        }
    }
}