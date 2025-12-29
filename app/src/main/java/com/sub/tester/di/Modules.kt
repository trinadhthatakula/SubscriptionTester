package com.sub.tester.di

import android.content.Context
import com.sub.tester.model.BillingHelper
import com.sub.tester.repo.SubscriptionManager
import com.sub.tester.vm.SubscriptionViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // 1. Inject SharedPreferences (Single instance for the whole app)
    single {
        androidContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    // 2. Inject BillingHelper
    single { BillingHelper(androidContext()) }

    // 3. Inject a Manager/Repository that observes billing and updates prefs
    // created as 'eager' single so it starts watching immediately if initialized
    single { SubscriptionManager(get(), get()) }

    viewModel { SubscriptionViewModel(get()) }
}