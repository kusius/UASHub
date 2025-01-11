package io.kusius.uashub

import io.kusius.klvmp.getPlatformKLVMP

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()