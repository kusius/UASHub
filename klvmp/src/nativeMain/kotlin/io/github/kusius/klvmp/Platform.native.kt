package io.github.kusius.klvmp

class NativePlatformKLVMP : PlatformKLVMP {
    override val name: String
        get() = "Native KLV MP"

    override fun createKLVParser(): KLVParser? {
        TODO("Not yet implemented")
    }

    override fun createTsDemuxer(): TsKLVDemuxer? {
        TODO("Not yet implemented")
    }
}


actual fun getPlatformKLVMP(): PlatformKLVMP = NativePlatformKLVMP()