package com.haskell.amghud.ble

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity

object BLEServiceManager {

    private var bleService: BLEService? = null
    private val boundActivities = mutableSetOf<AppCompatActivity>()

    fun bindToBleService(activity: AppCompatActivity, onBound: (BLEService) -> Unit, onUnbound: () -> Unit) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                val binder = service as BLEService.LocalBinder
                bleService = binder.getService()
                boundActivities.add(activity)
                onBound(bleService!!)
            }

            override fun onServiceDisconnected(arg0: ComponentName) {
                boundActivities.remove(activity)
                if (boundActivities.isEmpty()) {
                    bleService = null
                }
                onUnbound()
            }
        }

        Intent(activity, BLEService::class.java).also { intent ->
            activity.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    fun unbindFromBleService(activity: AppCompatActivity) {
        if (boundActivities.contains(activity)) {
            activity.unbindService(object : ServiceConnection{
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {}
                override fun onServiceDisconnected(name: ComponentName?) {}
            })
            boundActivities.remove(activity)
            if (boundActivities.isEmpty()) {
                bleService = null
            }
        }
    }

    fun getBleService(): BLEService? {
        return bleService
    }

    fun isServiceBound(): Boolean {
        return boundActivities.isNotEmpty()
    }
}