package com.harry12132.snoop

import android.app.Application
import com.harry12132.snoop.data.SnoopSettingsManager
import com.harry12132.snoop.manager.SnoopStateHub

class SnoopApp : Application() {

    lateinit var settingsManager: SnoopSettingsManager
        private set

    lateinit var stateHub: SnoopStateHub
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsManager = SnoopSettingsManager(this)
        stateHub = SnoopStateHub(this, settingsManager)
    }

    companion object {
        private var instance: SnoopApp? = null
        fun getInstance(): SnoopApp = instance!!
    }
}
