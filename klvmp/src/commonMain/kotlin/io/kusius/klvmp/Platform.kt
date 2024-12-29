package io.kusius.klvmp

enum class ValueType {
    STRING,
    INT,
    FLOAT,
    DOUBLE,
    LONG,
    UNKNOWN,
    PARSE_ERROR
}

// TODO: Make holistic data class UASInfo that holds all the available info from a KLV set
//  Needs to have a total of 141 members with their respective type populated or null
data class UASDataset(val a: Int) {
    companion object {
        fun fromKLVSet(klvElements: List<KLVElement>): UASDataset {
            return UASDataset(0)
        }
    }
}

// TODO: (When above is complete) Make KLVElement internal. We will only expose a
//  neat UASInfo class with all the info from a specific PES packet.
data class KLVElement(
    val key: Int,
    val length: Int,
    val valueType: ValueType,
    val valueBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as KLVElement

        if (key != other.key) return false
        if (length != other.length) return false
        if (valueType != other.valueType) return false
        if (!valueBytes.contentEquals(other.valueBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key
        result = 31 * result + length
        result = 31 * result + valueType.hashCode()
        result = 31 * result + valueBytes.contentHashCode()
        return result
    }
}

interface OnKLVBytesListener {
    fun onKLVBytesReceivedCallback(bytes: ByteArray)
}

interface KLVParser {
    fun parseKLVBytes(bytes: ByteArray): List<io.kusius.klvmp.KLVElement>
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