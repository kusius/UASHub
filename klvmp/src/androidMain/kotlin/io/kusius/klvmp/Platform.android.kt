package io.kusius.klvmp

data class KLVElement(
    val key: Int,
    val length: Int,
    val valueType: ValueType,
    private val valueBytes: ByteArray
)

class AndroidKLVParser(private val nativeHandle: Int) : KLVParser {
    private external fun parseKLV(nativeHandle: Int, bytes: ByteArray, length: Int,
                                  result: Array<KLVElement>, resultSize: Int)
    private external fun disposeParser(nativeHandle: Int)

    init {
        println("Allocated parser ${this.nativeHandle}")
    }

    override fun parseKLVBytes(bytes: ByteArray) {
        val resultSize = 512
        val result = Array(resultSize) {
            KLVElement(0,0,ValueType.UNKNOWN, byteArrayOf())
        }

        parseKLV(nativeHandle, bytes, bytes.size, result, resultSize)
    }

    override fun close() {
        disposeParser(nativeHandle)
    }

}

class AndroidPlatformKLVMP private constructor() : PlatformKLVMP {
    companion object {
        const val TAG = "AndroidPlatformKLVMP"

        @Volatile private var instance: AndroidPlatformKLVMP? = null

        @Synchronized
        fun getInstance(): AndroidPlatformKLVMP =
            instance ?: synchronized(this) {
                instance ?: AndroidPlatformKLVMP().also { instance = it }
            }
    }

    private external fun newKLVParser(): Int
    private external fun initNative()

    init {
        System.loadLibrary("klv")
        initNative()
    }

    override fun createKLVParser() : AndroidKLVParser? {
        val nativeHandle = newKLVParser()

        return if(nativeHandle >= 0) {
            AndroidKLVParser(nativeHandle = nativeHandle)
        } else null
    }

    override val name: String
        get() = "Android KLV MP"
}


actual fun getPlatformKLVMP(): PlatformKLVMP = AndroidPlatformKLVMP.getInstance()