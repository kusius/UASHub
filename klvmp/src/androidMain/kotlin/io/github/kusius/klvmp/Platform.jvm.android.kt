package io.github.kusius.klvmp

actual suspend fun loadNativeLibs() {
    System.loadLibrary("klv")
    System.loadLibrary("tsdemux")
}