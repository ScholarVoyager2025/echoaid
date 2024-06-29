package com.example.audiocapturer
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.audiocapturer.utils.MyLog
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private val client = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build()
    private lateinit var captureActivity: ActivityResultLauncher<Intent>

    private lateinit var barWidth: SeekBar
    private lateinit var barHeight: SeekBar
    private lateinit var barFontSize: SeekBar
    private lateinit var barAlpha: SeekBar

    private lateinit var textFloatingWindowInfo: TextView
    private lateinit var textHeight: TextView
    private lateinit var textWidth: TextView
    private lateinit var textFontSize: TextView
    private lateinit var textTransparency: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.mainTestButton)
            .setOnClickListener{
                launchAudioCapture()
            }

        findViewById<Button>(R.id.main_switch_background_1)
            .setOnClickListener{
                switchBackground(1)
            }

        findViewById<Button>(R.id.main_switch_background_2)
            .setOnClickListener{
                switchBackground(2)
            }

        findViewById<Button>(R.id.main_switch_background_3)
            .setOnClickListener{
                switchBackground(3)
            }

        findViewById<Button>(R.id.mainExportLog)
            .setOnClickListener{
                exportLogFileToDownload(getLogFile())
            }

        findViewById<Button>(R.id.mainShowHistory)
            .setOnClickListener{
                val intent = Intent(this, HistoryActivity::class.java)
                startActivity(intent)
            }

        barWidth = findViewById<SeekBar>(R.id.widthBar)
        barWidth
            .setOnSeekBarChangeListener(createSeekBarListener {
                updateFloatingAttributes(FloatingWindowAttributes(width = it))
            })

        barHeight = findViewById<SeekBar>(R.id.heightBar)
        barHeight
            .setOnSeekBarChangeListener(createSeekBarListener {
                updateFloatingAttributes(FloatingWindowAttributes(height = it))
            })

        barFontSize = findViewById<SeekBar>(R.id.fontSizeBar)
        barFontSize
            .setOnSeekBarChangeListener(createSeekBarListener {
                updateFloatingAttributes(FloatingWindowAttributes(fontSize = it.toFloat()))
            })

        barAlpha = findViewById<SeekBar>(R.id.alphaBar)
        barAlpha
            .setOnSeekBarChangeListener(createSeekBarListener {
                updateFloatingAttributes(FloatingWindowAttributes(alpha = it / 100.0f))
            })

        textFloatingWindowInfo = findViewById(R.id.mainLayoutFloatingInfo)
        textHeight = findViewById(R.id.mainHeightText)
        textWidth = findViewById(R.id.mainWidthText)
        textFontSize = findViewById(R.id.mainFontSizeText)
        textTransparency = findViewById(R.id.mainTransparencyText)


//        transcriptionTextView = findViewById(R.id.transcriptionTextView)
//        val startTranscriptionButton: Button = findViewById(R.id.startTranscriptionButton)
//        startTranscriptionButton.setOnClickListener {
//            playCapturing()
//            startTranscription()
//        }

        captureActivity =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
                val data = activityResult.data
                val resultCode = activityResult.resultCode

                if (resultCode != Activity.RESULT_OK) {
                    MyLog.e("reject", "用户拒绝采集")
                    Toast.makeText(this, "用户拒绝采集", Toast.LENGTH_SHORT).show()
//                    working = false
                    return@registerForActivityResult
                }

                //

                val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date()) // 使用当前时间
                val fileName = "$timestamp.txt"
                val resultFile = createFile(fileName)
                MyLog.i("history", "Create file: $fileName")

                val intent = Intent(this, AudioCaptureService::class.java)
                intent.putExtra("resultCode", resultCode)
                intent.putExtra("data", data)
                intent.putExtra(AudioCaptureService.FILE_PATH, resultFile.path)
                startForegroundService(intent)

                updateFloatingAttributes(
                    FloatingWindowAttributes(
                        width = barWidth.progress,
                        height = barHeight.progress,
                        alpha = 1 - barAlpha.progress / 100f,
                        fontSize = barFontSize.progress.toFloat()
                    )
                )
            }
    }

    private fun createFile(fileName: String): File {
        val historyDir = File(filesDir, "history")
        if (!historyDir.exists()) {
            historyDir.mkdir()
        }

        val file = File(historyDir, "$fileName.txt")

        if (!file.exists()) {
            file.createNewFile()
        }

        return file
    }

    override fun onStart() {
        super.onStart()
        showFloatingWindowInfo()
    }

    private fun setButtonsEnabled(isCapturingAudio: Boolean) {
//        findViewById<Button>(R.id.btn_start_recording).isEnabled = !isCapturingAudio
//        findViewById<Button>(R.id.btn_stop_recording).isEnabled = isCapturingAudio
    }

    private fun checkPermission() : Boolean{
        if (!isRecordAudioPermissionGranted()) {
            requestRecordAudioPermission()
            return false
        }
        if (!isForegroundPermissionGranted()) {
            requestForegroundPermission()
            return false
        }
        if(!isReadWriteExternalStoragePermissionGranted()) {
            requestWriteExternalStoragePermission()
            return false
        }

        return true

    }


    private fun stopCapturing() {
        setButtonsEnabled(isCapturingAudio = false)

        startService(Intent(this, AudioCaptureService::class.java).apply {
            action = AudioCaptureService.ACTION_STOP
        })
    }


    private fun playCapturing() {
        val audioFile = File(getExternalFilesDir(null), "/AudioCaptures/Capture.pcm")
        if(!audioFile.exists() || !audioFile.isFile()){
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    this, "No capture found.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }


        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
            AudioTrack.MODE_STREAM
        )

        val input = FileInputStream(audioFile)
        val data = ByteArray(bufferSize)// Calculate audio length in milliseconds
        val audioLength = (audioFile.length() / 2 / sampleRate * 1000).toInt() // Divide by 2 because it's 16-bit (2 bytes per sample)
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, "Audio Length: ${audioLength / 1000f} seconds", Toast.LENGTH_SHORT).show()
        }

        audioTrack.play()

        Thread {
            var read = input.read(data)
            while (read != -1) {
                audioTrack.write(data, 0, read)
                read = input.read(data)
            }
            audioTrack.stop()
            audioTrack.release()
            input.close()

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this, "播放完成", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun isRecordAudioPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isForegroundPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.FOREGROUND_SERVICE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isReadWriteExternalStoragePermissionGranted(): Boolean {
        return true
    }

    private fun requestRecordAudioPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            RECORD_AUDIO_PERMISSION_REQUEST_CODE
        )
    }

    private fun requestForegroundPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.FOREGROUND_SERVICE),
            FOREGROUND_REQUEST_CODE
        )
    }

    private fun requestWriteExternalStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
            WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    "Permissions to capture audio granted. Click the button once again.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this, "Permissions to capture audio denied.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        else if (requestCode == FOREGROUND_REQUEST_CODE) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    "Permissions to foreground granted. Click the button once again.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this, "Permissions to forground denied.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        else if(requestCode == WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE){
            if(grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    "Permissions to write external storage. Click the button once again.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this, "Permissions to write external storage denied.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Before a capture session can be started, the capturing app must
     * call MediaProjectionManager.createScreenCaptureIntent().
     * This will display a dialog to the user, who must tap "Start now" in order for a
     * capturing session to be started. This will allow both video and audio to be captured.
     */
    private fun startMediaProjectionRequest() {
        // use applicationContext to avoid memory leak on Android 10.
        // see: https://partnerissuetracker.corp.google.com/issues/139732252
        mediaProjectionManager =
            applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(),
            MEDIA_PROJECTION_REQUEST_CODE
        )
    }

    private fun launchAudioCapture() {
        if(!checkPermission()) return
        val fileWriter = openLogFile()
        MyLog.setLogWriter(fileWriter)
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        captureActivity.launch(captureIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MEDIA_PROJECTION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(
                    this,
                    "MediaProjection permission obtained. Foreground service will be started to capture audio.",
                    Toast.LENGTH_SHORT
                ).show()

                val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date()) // 使用当前时间
                val fileName = "history/$timestamp.txt"
                val file = createFile(fileName)
                MyLog.i("history", "Create file: $fileName")

                val audioCaptureIntent = Intent(this, AudioCaptureService::class.java).apply {
                    action = AudioCaptureService.ACTION_START
                    putExtra(AudioCaptureService.EXTRA_RESULT_DATA, data!!)
                    putExtra(AudioCaptureService.FILE_PATH, file.path)
                }
                startForegroundService(audioCaptureIntent)
            } else {
                Toast.makeText(
                    this, "Request to obtain MediaProjection denied.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun createSeekBarListener(updateFunction: (Int) -> Unit): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    updateFunction(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // 可以在这里添加逻辑
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 可以在这里添加逻辑
            }
        }
    }

    private fun switchBackground(backgroundId: Int) {
        updateFloatingAttributes(FloatingWindowAttributes(backgroundId = backgroundId))
    }
    private fun updateFloatingAttributes(attributes: FloatingWindowAttributes) {
        showFloatingWindowInfo()
        val intent = Intent(this.getString(R.string.com_example_floating_resize_event))
        intent.putExtra("attributes", attributes)
        sendBroadcast(intent)
        MyLog.i("broadcast", "已发送广播 intent")
    }

    private fun showFloatingWindowInfo() {
        val text = "宽度: ${barWidth.progress}\n高度${barHeight.progress}\n字体大小${barFontSize.progress}\n透明度${100 - barAlpha.progress}%"
        textFloatingWindowInfo.text = text

        textWidth.text = barWidth.progress.toString()
        textHeight.text = barHeight.progress.toString()
        textFontSize.text = barFontSize.progress.toString()
        textTransparency.text = (100 - barAlpha.progress).toString()
    }

    private fun openLogFile(): FileWriter {
        val fileWriter: FileWriter = FileWriter(getLogFile(), true)
        return fileWriter
    }

    private fun getLogFile() : File {
        val rootDir = getExternalFilesDir(null)
        val logDir = File(rootDir, "dhhx/logs")
        if(!logDir.exists()) {
            val succ = logDir.mkdirs()
            if(!succ) {
                MyLog.e("storage", "create file ${logDir.absolutePath} failed")
                throw IOException("无法创建目录：${logDir.absolutePath}")
            }
        }

        val logFile = File(logDir, "log.txt")
        if(!logFile.exists()) logFile.createNewFile()
        return logFile
    }
    private fun exportLogFileToDownload(privateFile: File){
        val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val publicFile = File(publicDir, privateFile.name)

        try {
            FileInputStream(privateFile).use { input ->
                FileOutputStream(publicFile).use { output ->
                    input.copyTo(output)
                }
            }
            // 通知用户文件已导出
            Log.i("log","File exported to: ${publicFile.absolutePath}")
            Toast.makeText(this, "已导出到: ${publicFile.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 42
        private const val FOREGROUND_REQUEST_CODE = 43
        private const val WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 44
        private const val MEDIA_PROJECTION_REQUEST_CODE = 13
    }
}
