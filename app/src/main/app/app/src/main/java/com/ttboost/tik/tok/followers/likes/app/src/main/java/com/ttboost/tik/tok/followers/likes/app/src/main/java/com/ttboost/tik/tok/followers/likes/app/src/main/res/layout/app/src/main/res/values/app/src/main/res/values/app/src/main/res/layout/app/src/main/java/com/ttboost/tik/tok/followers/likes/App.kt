package com.ttboost.tik.tok.followers.likes

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        SecurityUtils.performSecurityChecks(this)
    }
}

