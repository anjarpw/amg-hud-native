package com.haskell.amghud

import android.app.Service


interface ILocalBinder<T : Service> {
    fun getService(): T
}
