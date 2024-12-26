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

interface KLVParser {
    fun parseKLVBytes(bytes: ByteArray): Unit
    fun close(): Unit
}

interface PlatformKLVMP {
    val name: String
    fun createKLVParser(): KLVParser?
}

expect fun getPlatformKLVMP(): PlatformKLVMP