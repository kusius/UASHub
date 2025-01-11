package io.kusius.klvmp

actual suspend fun loadNativeLibs() {
    System.loadLibrary("klv")
    System.loadLibrary("tsdemux")
}