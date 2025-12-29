package com.sub.tester.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.sub.tester.vm.SubscriptionViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Handle User Messages (Side Effects)
    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { message ->
            snackBarHostState.showSnackbar(message)
            viewModel.messageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = {
            // Added WindowInsets handling so it doesn't clip status bar
            Box(modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(16.dp)) {
                Text(
                    text = "Store",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.subscriptions.isNotEmpty()) {
                        item { SectionHeader("Subscriptions") }
                        items(state.subscriptions) { product ->
                            ProductItem(product) {
                                (context as? Activity)?.let { viewModel.buyProduct(it, product) }
                            }
                        }
                    }

                    if (state.oneTimeProducts.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionHeader("One-Time Purchases")
                        }
                        items(state.oneTimeProducts) { product ->
                            ProductItem(product) {
                                (context as? Activity)?.let { viewModel.buyProduct(it, product) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductItem(
    product: ProductDetails,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Price Button
            Button(onClick = onClick) {
                Text(text = product.getFormattedPrice())
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

/**
 * EXTENSION FUNCTION: Extracts the price from the messy ProductDetails object.
 * This prevents your UI code from looking like spaghetti.
 */
fun ProductDetails.getFormattedPrice(): String {
    return try {
        if (productType == BillingClient.ProductType.INAPP) {
            oneTimePurchaseOfferDetails?.formattedPrice ?: "N/A"
        } else {
            // For subscriptions, we take the first offer's first pricing phase.
            // In a real app, you might have multiple offers (monthly, yearly, trial).
            // You should implement logic to pick the CORRECT offer.
            subscriptionOfferDetails?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.firstOrNull()
                ?.formattedPrice ?: "N/A"
        }
    } catch (_: Exception) {
        "Err"
    }
}