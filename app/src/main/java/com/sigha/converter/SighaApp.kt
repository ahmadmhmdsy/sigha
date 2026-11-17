package com.sigha.converter

import android.app.Application

/**
 * Application entry point. Initialises the conversion graph once per process.
 */
class SighaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}
