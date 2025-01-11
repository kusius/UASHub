package io.kusius.uashub

import io.kusius.klvmp.getPlatformKLVMP

class Greeting {
    private val platform = getPlatformKLVMP()
    private val parser = platform.createKLVParser()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}