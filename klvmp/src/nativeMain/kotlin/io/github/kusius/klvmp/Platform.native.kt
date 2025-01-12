package io.kusius.klvmp

class NativePlatformKLVMP : PlatformKLVMP {
    override val name: String
        get() = "Native KLV MP"
}


actual fun getPlatformKLVMP(): PlatformKLVMP = NativePlatformKLVMP()