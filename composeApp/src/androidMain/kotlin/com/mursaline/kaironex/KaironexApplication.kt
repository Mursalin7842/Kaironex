package com.mursaline.kaironex

import android.app.Application
import com.mursaline.kaironex.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class KaironexApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger()
            androidContext(this@KaironexApplication)
            modules(appModule)
        }
    }
}
