package io.kusius.klvmp

class DesktopPlatformKLVMP : PlatformKLVMP {
    override val name: String
        get() = "Desktop KLV MP"
}


actual fun getPlatformKLVMP(): PlatformKLVMP = DesktopPlatformKLVMP()