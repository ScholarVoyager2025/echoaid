package com.example.audiocapturer

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.audiocapturer.utils.Ernie
import com.example.audiocapturer.utils.Ernie.Companion.jsonSystem
import com.example.audiocapturer.utils.MyLog
import com.example.audiocapturer.utils.getWsUrl
import com.lzf.easyfloat.EasyFloat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.experimental.and


class AudioCaptureService : Service() {
    private var audioFormat: AudioFormat? = null
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private val httpClient: OkHttpClient = OkHttpClient.Builder().build();

    private var audioRecord: AudioRecord? = null
    var audioCaptureThread: Thread? = null
    var startTime: LocalDateTime? = null
    private lateinit var ernie: Ernie

    private lateinit var logic: AudioCaptureLogic
    var isCapture: Boolean = false
    val llmTextResults = ArrayDeque<String>()

    init {
        GlobalScope.launch(Dispatchers.IO) {
            ernie = Ernie()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(
            SERVICE_ID, NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).build()
        )
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Audio Capture Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val manager = getSystemService(NotificationManager::class.java) as NotificationManager
        manager.createNotificationChannel(serviceChannel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        mediaProjectionManager =
            applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        mediaProjection = mediaProjectionManager.getMediaProjection(
            intent!!.getIntExtra("resultCode", Activity.RESULT_OK),
            intent.getParcelableExtra("data")!!
        )

        logic = AudioCaptureLogic(this)
        logic.showFloatingWindow()

        return super.onStartCommand(intent, flags, startId)
    }


    fun startAudioCapture() {
        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA) // TODO provide UI options for inclusion/exclusion
            .build()

        /**
         * Using hardcoded values for the audio format, Mono PCM samples with a sample rate of 8000Hz
         * These can be changed according to your application's needs
         */
        audioFormat =
            audioFormat ?: AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(16000).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build()

        audioRecord = audioRecord ?: AudioRecord.Builder().setAudioFormat(audioFormat!!)
            // For optimal performance, the buffer size
            // can be optionally specified to store audio samples.
            // If the value is not specified,
            // uses a single frame and lets the
            // native code figure out the minimum buffer size.
            .setBufferSizeInBytes(BUFFER_SIZE_IN_BYTES).setAudioPlaybackCaptureConfig(config)
            .build()

        audioRecord!!.startRecording()

        startTime = LocalDateTime.now()

        audioCaptureThread = thread(start = true) {
            startTranscription()
        }
    }

    private fun createAudioFile(): File {
        val audioCapturesDirectory = File(getExternalFilesDir(null), "/AudioCaptures")
        if (!audioCapturesDirectory.exists()) {
            audioCapturesDirectory.mkdirs()
        }
        val timestamp = SimpleDateFormat("dd-MM-yyyy-hh-mm-ss", Locale.US).format(Date())
//        val fileName = "Capture-$timestamp.pcm"
        val fileName = "Capture.pcm"
        return File(audioCapturesDirectory.absolutePath + "/" + fileName)
    }

    private fun startTranscription() {
        val capturedAudioSamples = ShortArray(NUM_SAMPLES_PER_READ)

        val wsRequest = Request.Builder().url(getWsUrl()).build()
        val listener = createWebSocketListener()

        val webSocket = try {
            httpClient.newWebSocket(wsRequest, listener)
        } catch (e: Exception) {
            MyLog.e("network", "创建 WebSocket 出错", e)
            e.printStackTrace()
            throw e
        }

        try {
            while (this.audioCaptureThread?.isInterrupted == false) {
                val bytesRead: Int =
                    audioRecord!!.read(capturedAudioSamples, 0, NUM_SAMPLES_PER_READ)
                if (bytesRead > 0) {
                    val bytes = capturedAudioSamples.toByteStringLe()
                    val sendSuccess = webSocket.send(bytes)
//                MyLog.d("send", "send success: $sendSuccess")
                    if (!sendSuccess) break
                }
            }
        } finally {
            webSocket.close(1000, "结束捕获")
        }
    }

    fun stopAudioCapture() {
        requireNotNull(mediaProjection) { "Tried to stop audio capture, but there was no ongoing capture in place!" }

        audioCaptureThread?.interrupt()
        audioCaptureThread?.join()

        audioRecord!!.stop()

//        mediaProjection!!.stop()
    }

    override fun onBind(p0: Intent?): IBinder? = null

    private fun ShortArray.toByteArray(): ByteArray {
        // Samples get translated into bytes following little-endianness:
        // least significant byte first and the most significant byte last
        val bytes = ByteArray(size * 2)
        for (i in 0 until size) {
            bytes[i * 2] = (this[i] and 0x00FF).toByte()
            bytes[i * 2 + 1] = (this[i].toInt() shr 8).toByte()
            this[i] = 0
        }
        return bytes
    }

    private fun onMessage(message: TranscriptionResp) {
        if (message is TranscriptionMessage) logic.newTranscription(message)
    }

    fun deactivate() {
        mediaProjection?.stop()
        EasyFloat.dismiss()

        stopSelf()
    }

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                MyLog.i("msg", "Socket Open")
            }

            /**
             * 语音转文字信息
             */
            override fun onMessage(webSocket: WebSocket, text: String) {
                val resp = TranscriptionResp.Create(text)
                if (resp is TranscriptionMessage) {
                    MyLog.d("msg", "New Response")
                    this@AudioCaptureService.onMessage(resp)
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // Not used
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
//            runOnUiThread { Toast.makeText(this@MainActivity, "WebSocket Closed", Toast.LENGTH_SHORT).show() }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
//            runOnUiThread { Toast.makeText(this@MainActivity, "WebSocket Failure: ${t.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    fun clearContext() {
        llmTextResults.clear()
    }

    suspend fun callLlm() {
        val input = logic.wordContainer.getText()
        val inputRecent = logic.wordContainer.getTextSince(LocalDateTime.now().minusSeconds(40L))
        MyLog.i("llm", "提示词1: $inputRecent")
        MyLog.i("llm", "提示词2: $input")

        /** 流式响应
//        val channel = Channel<String> {  }
        val respLines = ernie.executeStream(1, inputRecent)
//        if(respTextStream == null) {
//            logic.newConcentration("LLM 异常")
//            MyLog.e("llm", "LLM 异常，返回了空")
//            return
//        }
        MyLog.i("llm", "流式请求成功")
        */

        val firstResp = ernie.execute(1, inputRecent)
        if(firstResp == null) {
            logic.newConcentration("LLM 异常")
            MyLog.e("llm", "LLM 异常，返回了空")
            return
        }
        logic.newConcentration(firstResp)
        llmTextResults.addLast(firstResp)

        val emojiResp =  ernie.execute(arrayOf(inputRecent, firstResp, Ernie.emojiSystem), topP = 0.1f)
//        val emojiResp = ernie.execute(arrayOf(inputRecent), topP = 0.1f)
        MyLog.i("llm", "Emoji: $emojiResp")
        emojiResp?.let{ logic.newEmojis(it) }

        MyLog.d("llm", "添加 llmTextResult: $firstResp")
        while(llmTextResults.size > 100) llmTextResults.removeFirst()

        val jsonInput = llmTextResults.takeLast(10).mapIndexed {i, s -> "$i: $s"}
            .joinToString(separator = "\n") { it } + jsonSystem

        val jsonResp = ernie.execute(2, jsonInput);
        logic.newJson(jsonResp ?: "{}")
    }

    companion object {
        private const val LOG_TAG = "AudioCaptureService"
        private const val SERVICE_ID = 123
        private const val NOTIFICATION_CHANNEL_ID = "AudioCapture channel"

        private const val NUM_SAMPLES_PER_READ = 16000 * 40 / 1000  // 16000Hz * 40ms = 640
        private const val BYTES_PER_SAMPLE = 2 // 2 bytes since we hardcoded the PCM 16-bit format
        private const val BUFFER_SIZE_IN_BYTES = NUM_SAMPLES_PER_READ * BYTES_PER_SAMPLE

        const val ACTION_START = "AudioCaptureService:Start"
        const val ACTION_STOP = "AudioCaptureService:Stop"
        const val EXTRA_RESULT_DATA = "AudioCaptureService:Extra:ResultData"
    }
}

private fun ShortArray.toByteStringLe(): ByteString {
    val byteArray = ByteArray(this.size * 2)
    for (i in this.indices) {
        byteArray[2 * i] = (this[i].toInt() and 0xFF).toByte()          // 低位字节
        byteArray[2 * i + 1] = (this[i].toInt() shr 8 and 0xFF).toByte() // 高位字节
    }

    val byteString = byteArray.toByteString(0, byteArray.size)
    return byteString
}
