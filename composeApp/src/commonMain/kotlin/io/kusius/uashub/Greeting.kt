package io.kusius.uashub

import io.kusius.klvmp.getPlatformKLVMP

class Greeting {
    private val platform = getPlatformKLVMP()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}