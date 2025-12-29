package com.sub.tester

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sub.tester.ui.SubscriptionScreen
import com.sub.tester.ui.theme.SubscriptionTesterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SubscriptionTesterTheme {
                SubscriptionScreen()
            }
        }
    }
}
