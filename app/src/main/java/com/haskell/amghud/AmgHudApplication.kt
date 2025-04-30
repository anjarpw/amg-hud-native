package com.haskell.amghud

import android.app.Application

class AmgHudApplication : Application() {
    var bleState: BLEState = BLEState()
        private set // Make it read-only from outside


    override fun onCreate() {
        super.onCreate()
        // Initialize other global resources here if needed
    }

    fun updateBLEState(newBLEState: BLEState) {
        bleState = newBLEState
    }


}