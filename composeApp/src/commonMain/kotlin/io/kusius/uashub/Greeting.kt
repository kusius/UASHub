package io.kusius.uashub

import io.kusius.klvmp.getPlatformKLVMP

class Greeting {
    private val platform = getPlatformKLVMP()

    fun greet(): String {
        val parser = platform.createKLVParser()
        println("Created parser: $parser")
        parser?.parseKLVBytes(byteArrayOf())

        return "Hello, ${platform.name}!"
    }
}