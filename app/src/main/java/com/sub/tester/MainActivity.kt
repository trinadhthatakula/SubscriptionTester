package com.sub.tester

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.sub.tester.model.BillingHelper
import com.sub.tester.model.productList
import com.sub.tester.ui.screens.MainScreen
import com.sub.tester.ui.theme.SubscriptionTesterTheme

class MainActivity : ComponentActivity() {

    val billingHelper = BillingHelper.getInstance(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SubscriptionTesterTheme {

                val productList by billingHelper.productDetailsList.collectAsStateWithLifecycle()
                val activeSubscriptions by billingHelper.activeSubsList.collectAsStateWithLifecycle()
                val oneTimePurchases by billingHelper.oneTimeList.collectAsStateWithLifecycle()

                var statusTxt by remember { mutableStateOf(billingHelper.lastState) }

                billingHelper.stateObserver = {
                    statusTxt = it
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        statusTxt = statusTxt,
                        productList = productList,
                        activeSubsList = activeSubscriptions,
                        oneTimeList = oneTimePurchases
                    )
                }

            }
        }
    }
}
