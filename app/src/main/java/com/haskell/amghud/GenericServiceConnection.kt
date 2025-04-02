package com.haskell.amghud

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder


class GenericServiceConnection<T : Service?>(
    private val context: Context,
    private val serviceClass: Class<T>,
    listener: ServiceConnectionListener<T?>
) :
    ServiceConnection {
    var service: T? = null
        private set
    var isBound: Boolean = false
        private set
    private val listener: ServiceConnectionListener<T?>

    interface ServiceConnectionListener<T> {
        fun onServiceConnected(service: T)
        fun onServiceDisconnected()
    }

    init {
        this.listener = listener
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
