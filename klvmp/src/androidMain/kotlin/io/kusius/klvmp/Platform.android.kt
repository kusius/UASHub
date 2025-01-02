package io.kusius.klvmp

import android.util.Log
import io.kusius.klvmp.AndroidPlatformKLVMP.Companion.TAG

internal class AndroidKLVParser(private val nativeHandle: Int) : KLVParser {
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
            KLVElement(0,0,ValueType.UNKNOWN, byteArrayOf(), UnknownValue)
        }

        val parsedCount = parseKLV(nativeHandle, bytes, result, resultSize)

        val elements = if(parsedCount > 0) result.take(parsedCount) else emptyList()

        return UASDataset.fromKLVSet(elements)
    }

    override fun close() {
        disposeParser(nativeHandle)
    }

}

internal class AndroidTsKLVDemuxer(private val nativeHandle: Int)  : TsKLVDemuxer{
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
        Log.d(TAG, "Received ${data.size} from Ts Demuxer")
        onKLVBytesListener?.onKLVBytesReceivedCallback(data)
    }
    override fun close() {
        disposeDemuxer(nativeHandle)
    }
}

internal class AndroidPlatformKLVMP private constructor() : PlatformKLVMP {
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
    private external fun newTsDemuxer(): Int

    init {
        System.loadLibrary("klv")
        System.loadLibrary("tsdemux")
    }

    override fun createKLVParser() : AndroidKLVParser? {
        val nativeHandle = newKLVParser()

        return if(nativeHandle >= 0) {
            AndroidKLVParser(nativeHandle = nativeHandle)
        } else null
    }

    override fun createTsDemuxer(): TsKLVDemuxer? {
        val nativeHandle = newTsDemuxer()

        return if(nativeHandle >= 0) {
            AndroidTsKLVDemuxer(nativeHandle = nativeHandle)
        } else null
    }

    override val name: String
        get() = "Android KLV MP"
}


actual fun getPlatformKLVMP(): PlatformKLVMP = AndroidPlatformKLVMP.getInstance()