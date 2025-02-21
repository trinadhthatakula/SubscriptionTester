package com.sub.tester.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    productList: List<ProductDetails> = emptyList(),
    activeSubsList: List<Purchase> = emptyList(),
    oneTimeList: List<Purchase> = emptyList()
) {
    Column(modifier.fillMaxSize()) {
        LazyColumn {
            items(productList) { product ->
                if (product.productType == BillingClient.ProductType.INAPP)
                    ListItem(
                        headlineContent = {
                            Text(text = product.name)
                        },
                        supportingContent = {
                            Text(text = product.description)
                        },
                        trailingContent = {
                            Text(text = product.oneTimePurchaseOfferDetails?.formattedPrice ?: "")
                        }
                    )
                else {
                    ListItem(
                        headlineContent = {
                            Text(text = product.name)
                        },
                        supportingContent = {
                            Column (modifier = Modifier.fillMaxWidth()){
                                product.subscriptionOfferDetails?.forEach { subOffer ->
                                    subOffer.pricingPhases.pricingPhaseList.forEach { phase ->
                                        ListItem(
                                            headlineContent = {
                                                Text(
                                                    phase.billingPeriod
                                                )
                                            },
                                            trailingContent = {
                                                Text(text = phase.formattedPrice)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}