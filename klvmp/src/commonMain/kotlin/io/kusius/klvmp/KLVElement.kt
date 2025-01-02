package io.kusius.klvmp


internal enum class ValueType {
    STRING,
    INT,
    FLOAT,
    DOUBLE,
    LONG,
    UNKNOWN,
    PARSE_ERROR
}

internal sealed class KLVValue(open val value: Any) {
    fun toStringValue(): String? {
        return if(this is StringValue) this.value else null
    }

    fun toFloatValue(): Float? {
        return if(this is FloatValue) this.value else null
    }

    fun toDoubleValue(): Double? {
        return if(this is DoubleValue) this.value else null
    }

    fun toLongValue(): Long? {
        return if(this is LongValue) this.value else null
    }

    fun toIntValue(): Int? {
        return if(this is IntValue) this.value else null
    }

    fun toBoolean(): Boolean? {
        return if(this is IntValue) {
            when (this.value) {
                0 -> false
                1 -> true
                else -> null
            }
        } else null
    }
}
internal data class IntValue(override val value: Int) : KLVValue(value)
internal data class FloatValue(override val value: Float) : KLVValue(value)
internal data class DoubleValue(override val value: Double) : KLVValue(value)
internal data class StringValue(override val value: String) : KLVValue(value)
internal data class LongValue(override val value: Long) : KLVValue(value)
internal data object UnknownValue : KLVValue(0)
internal data object ParseErrorValue: KLVValue(0)

internal data class KLVElement(
    val key: Int,
    val length: Int,
    val valueType: ValueType,
    val valueBytes: ByteArray,
    val value: KLVValue
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as KLVElement

        if (key != other.key) return false
        if (length != other.length) return false
        if (valueType != other.valueType) return false
        if (!valueBytes.contentEquals(other.valueBytes)) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key
        result = 31 * result + length
        result = 31 * result + valueType.hashCode()
        result = 31 * result + valueBytes.contentHashCode()
        result = 31 * result + value.hashCode()
        return result
    }

}