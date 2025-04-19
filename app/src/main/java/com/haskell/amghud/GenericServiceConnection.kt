package com.haskell.amghud

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder


@Suppress("UNCHECKED_CAST")
class GenericServiceConnection<T : Service?>(
    private val context: Context,
    private val serviceClass: Class<T>,
    private val listener: ServiceConnectionListener<T?>
) :
    ServiceConnection {
    private var service: T? = null
    private var isBound: Boolean = false

    interface ServiceConnectionListener<T> {
        fun onServiceConnected(service: T)
        fun onServiceDisconnected()
    }

    fun bindService() {
        if (!isBound) {
            val intent = Intent(context, serviceClass)
            context.bindService(intent, this, Context.BIND_AUTO_CREATE)
        }
    }

    fun unbindService() {
        if (isBound) {
            context.unbindService(this)
            isBound = false
            service = null
            listener.onServiceDisconnected()
        }
    }

    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        if (binder is Binder) {
            service = (binder as ILocalBinder<*>).getService() as T
            isBound = true
            listener.onServiceConnected(service)
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        isBound = false
        service = null
        listener.onServiceDisconnected()
    }
}
