package com.example.arcoreaugmentedimage

import android.app.Application
import android.content.Context
import com.example.arcoreaugmentedimage.util.PreferenceProvider
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.androidXModule
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

class MainApplication : Application(), KodeinAware {

    companion object {
        private var instance: MainApplication? = null
        fun getInstance(): MainApplication? {
            return instance
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override val kodein = Kodein.lazy {

        import(androidXModule(this@MainApplication))
        bind() from singleton { PreferenceProvider(instance()) }
    }
}