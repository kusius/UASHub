package io.kusius.uashub

import android.os.Build

class AndroidPlatform : Platform {
    external fun parseKLV(): Unit

    init {
        System.loadLibrary("klv")
        parseKLV()
    }

    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()