package com.example.wellbeingplayground

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber


// using dagger/ hilt/ timber injection
@HiltAndroidApp
class BaseApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}