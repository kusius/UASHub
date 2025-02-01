package io.github.kusius.klvmp


interface OnKLVBytesListener {
    fun onKLVBytesReceivedCallback(bytes: ByteArray)
}

interface KLVParser {
    fun parseKLVBytes(bytes: ByteArray): UASDataset
    fun close(): Unit
}

interface TsKLVDemuxer {
    fun close()
    fun demuxKLV(streamData: ByteArray): ByteArray
    fun setOnKLVBytesListener(onKLVBytesListener: OnKLVBytesListener)
}

interface PlatformKLVMP {
    val name: String
    fun createKLVParser(): KLVParser?
    fun createTsDemuxer(): TsKLVDemuxer?
}

expect fun getPlatformKLVMP(): PlatformKLVMP