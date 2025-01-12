package io.github.kusius.klvmp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

internal class JVMKLVParser(private val nativeHandle: Int) : KLVParser {
    private external fun parseKLV(nativeHandle: Int, bytes: ByteArray,
                                  result: Array<KLVElement>,
                                  resultSize: Int) : Int
    private external fun disposeParser(nativeHandle: Int)

    init {
        println("Allocated parser ${this.nativeHandle}")
    }

    override fun parseKLVBytes(bytes: ByteArray) : UASDataset {
        // Allocate some memory for the native function to write the results if any
        val resultSize = 512
        val result = Array(resultSize) {
            KLVElement(0,0, ValueType.UNKNOWN, byteArrayOf(), UnknownValue)
        }

        val parsedCount = parseKLV(nativeHandle, bytes, result, resultSize)

        val elements = if(parsedCount > 0) result.take(parsedCount) else emptyList()

        return UASDataset.fromKLVSet(elements)
    }

    override fun close() {
        disposeParser(nativeHandle)
    }

}

internal class JVMTsKLVDemuxer(private val nativeHandle: Int)  : TsKLVDemuxer {
    private external fun disposeDemuxer(nativeHandle: Int)
    private external fun demuxKLVNative(nativeHandle: Int, streamData: ByteArray)
    private external fun registerCallback(nativeHandle: Int)

    private var onKLVBytesListener: OnKLVBytesListener? = null


    init {
        registerCallback(nativeHandle)
    }

    override fun demuxKLV(streamData: ByteArray): ByteArray {
        demuxKLVNative(nativeHandle, streamData)
        return byteArrayOf()
    }

    override fun setOnKLVBytesListener(onKLVBytesListener: OnKLVBytesListener) {
        this.onKLVBytesListener = onKLVBytesListener
    }

    fun onKLVBytesReceived(data: ByteArray) {
        onKLVBytesListener?.onKLVBytesReceivedCallback(data)
    }
    override fun close() {
        disposeDemuxer(nativeHandle)
    }
}

expect suspend fun loadNativeLibs()

internal open class JVMPlatformKLVMP : PlatformKLVMP {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    companion object {
        const val TAG = "AndroidPlatformKLVMP"



        @Volatile private var instance: JVMPlatformKLVMP? = null

        @Synchronized
        fun getInstance(): JVMPlatformKLVMP =
            instance ?: synchronized(this) {
                instance ?: JVMPlatformKLVMP().also { instance = it }
            }
    }

    private external fun newKLVParser(): Int
    private external fun newTsDemuxer(): Int

    init {
        // We block the constructor so as to return the instance
        // when it has loaded the native libs and is ready.
        // Otherwise we have race conditions
        runBlocking {
            withContext(Dispatchers.IO) {
                loadNativeLibs()
            }
        }
    }

    override fun createKLVParser() : JVMKLVParser? {
        val nativeHandle = newKLVParser()

        return if(nativeHandle >= 0) {
            JVMKLVParser(nativeHandle = nativeHandle)
        } else null
    }

    override fun createTsDemuxer(): TsKLVDemuxer? {
        val nativeHandle = newTsDemuxer()

        return if(nativeHandle >= 0) {
            JVMTsKLVDemuxer(nativeHandle = nativeHandle)
        } else null
    }

    override val name: String
        get() = "JVM KLV MP"
}

actual fun getPlatformKLVMP(): PlatformKLVMP = JVMPlatformKLVMP.getInstance()