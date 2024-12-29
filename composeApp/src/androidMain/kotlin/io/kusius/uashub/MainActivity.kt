package io.kusius.uashub

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ContentDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.extractor.DefaultExtractorInput
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.DummyTrackOutput
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.NoOpExtractorOutput
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.TrackOutput
import androidx.media3.extractor.text.DefaultSubtitleParserFactory
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.ts.TsExtractor
import io.kusius.klvmp.OnKLVBytesListener
import io.kusius.klvmp.PlatformKLVMP
import io.kusius.klvmp.ValueType
import io.kusius.klvmp.getPlatformKLVMP
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.EOFException
import java.nio.ByteBuffer
import javax.security.auth.login.LoginException
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val uri = Uri.parse("https://samples.ffmpeg.org/MPEG2/mpegts-klv/Day%20Flight.mpg")
                val dataSource = DefaultHttpDataSource.Factory().createDataSource()
                dataSource.open(DataSpec(uri))

                val demuxer = getPlatformKLVMP().createTsDemuxer()
                demuxer?.setOnKLVBytesListener(
                    object : OnKLVBytesListener {
                        private val klvParser = getPlatformKLVMP().createKLVParser()
                        override fun onKLVBytesReceivedCallback(bytes: ByteArray) {
                            val klvElements = klvParser?.parseKLVBytes(bytes)
                            klvElements?.forEach {
                                if(it.valueType == ValueType.STRING) {
                                    Log.d("MainActivity", "$it ${String(it.valueBytes)}")
                                } else {
                                    Log.d("MainActivity", "$it")
                                }

                            }
                        }
                    }
                )
                val bytes = ByteArray(1024)
                var totalBytesRead = 0
                while(isActive) {
                    val read = dataSource.read(bytes, 0, 1024)
                    if(read == C.RESULT_END_OF_INPUT) {
                        Log.d("MainActivity", "end of input")
                        break;
                    } else {
                        totalBytesRead += read
                        demuxer?.demuxKLV(bytes)
                    }
                }

                Log.d("MainActivity", "Total Bytes read $totalBytesRead")

            }
        }

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}