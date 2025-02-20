package com.sub.tester.model

import android.R.attr.type
import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.queryProductDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

var productList = listOf(
    ProductData("weekly_sub", BillingClient.ProductType.SUBS),
    ProductData("monthly_sub", BillingClient.ProductType.SUBS),
    ProductData("yearly_sub", BillingClient.ProductType.SUBS),
    ProductData("remove_ads", BillingClient.ProductType.INAPP),
    ProductData("premium_user", BillingClient.ProductType.INAPP),
)

data class ProductData(
    val id: String,
    val type: String
)

fun ProductData.toProductDetailsParams(): QueryProductDetailsParams.Product =
    QueryProductDetailsParams.Product.newBuilder()
        .setProductId(id)
        .setProductType(type)
        .build()

class BillingHelper(context: Context) {

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            // To be implemented in a later section.
        }

    private var billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .build()

    var productDetails: List<ProductDetails> = emptyList()

    init {
        startConnection {
            CoroutineScope(Dispatchers.IO).launch {
                fetchProductDetails(productList) { result ->
                    if (result.isSuccess) {
                        productDetails = result.getOrDefault(emptyList())
                    } else {
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails, offerToken: String? = null){

    }

    suspend fun fetchProductDetails(
        productList: List<ProductData>,
        onResult: ((Result<List<ProductDetails>>) -> Unit)? = null
    ) {
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
            } else {
                onResult?.invoke(Result.failure(Exception("Error fetching product details")))
            }
        }
    }

    var connected: Int = BillingClient.ConnectionState.CLOSED

    fun startConnection(onConnected: (() -> Unit)? = null) {
        if (billingClient.connectionState == BillingClient.ConnectionState.CONNECTED) {
            onConnected?.invoke()
        } else {
            billingClient.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingServiceDisconnected() {
                        connected = BillingClient.ConnectionState.DISCONNECTED
                    }

                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (billingResult.responseCode == BillingResponseCode.OK) {
                            // The BillingClient is ready. You can query purchases here.
                            connected = BillingClient.ConnectionState.CONNECTED
                        }
                    }

                }
            )
        }
    }


}