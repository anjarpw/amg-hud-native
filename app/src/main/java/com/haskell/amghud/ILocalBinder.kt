package com.haskell.amghud

import android.app.Service
import android.os.IBinder

interface ILocalBinder<T : Service> {
    fun getService(): T
}
