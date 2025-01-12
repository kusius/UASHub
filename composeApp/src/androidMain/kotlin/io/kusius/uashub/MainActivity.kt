package io.kusius.uashub

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import io.github.kusius.klvmp.OnKLVBytesListener
import io.github.kusius.klvmp.getPlatformKLVMP
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val uri = Uri.parse("https://samples.ffmpeg.org/MPEG2/mpegts-klv/Day%20Flight.mpg")
                val dataSource = DefaultHttpDataSource.Factory().createDataSource()
                dataSource.open(DataSpec(uri))

                val demuxer = getPlatformKLVMP().createTsDemuxer() ?: return@withContext
                val klvParser = getPlatformKLVMP().createKLVParser() ?: return@withContext

                demuxer?.setOnKLVBytesListener(
                    object : OnKLVBytesListener {
                        override fun onKLVBytesReceivedCallback(bytes: ByteArray) {
                            val uasDataset = klvParser?.parseKLVBytes(bytes)
                            Log.d("MainActivity", "$uasDataset")
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
                        demuxer.demuxKLV(bytes)
                    }
                }

                Log.d("MainActivity", "Total Bytes read $totalBytesRead")
                demuxer.close()
                klvParser.close()
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