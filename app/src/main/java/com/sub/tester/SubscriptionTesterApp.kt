package com.sub.tester

import android.app.Application
import com.sub.tester.di.appModule
import com.sub.tester.repo.SubscriptionManager
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class SubscriptionTesterApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            // Log Koin errors
            androidLogger(Level.ERROR)
            // Reference Android context
            androidContext(this@SubscriptionTesterApp)
            // Load modules
            modules(appModule)
        }

        // Force initialization of the SubscriptionManager so it starts watching immediately
        // (Only necessary if you want it running before any UI appears)
        val manager: SubscriptionManager by inject()
        // Accessing 'manager' here forces Koin to create the instance and run the init block
        manager.toString()
    }
}