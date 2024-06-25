package com.example.audiocapturer

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.text.SpannableString
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.audiocapturer.utils.MyLog
import com.example.audiocapturer.utils.RepeatingTask
import com.example.audiocapturer.utils.WordContainer
import com.example.audiocapturer.utils.indexesOf
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.enums.ShowPattern
import com.vdurmont.emoji.EmojiParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.time.LocalDateTime
import kotlin.random.Random

class AudioCaptureLogic(private val context: AudioCaptureService){
    companion object {

    }

    private var textType = 1
    val wordContainer = WordContainer()
    private val repeatingTask: RepeatingTask = RepeatingTask(30000) {
        callLlm()
    }


    private lateinit var floatingWindowLayout: LinearLayout
    lateinit var text1: TextView
    lateinit var textConcentration: TextView
    lateinit var textJson: TextView
    private lateinit var backgroundLayout: View
    private lateinit var buttonStartStop: Button
    private lateinit var buttonClear: Button
//    private lateinit var buttonLlm: Button
    private lateinit var buttonSwitch: Button
    private lateinit var handler: Handler
    private lateinit var emojiContainer: FrameLayout

    private val hideButtons: () -> Unit = {
        MyLog.d("ui", "hide buttons")
        buttonStartStop?.visibility = View.INVISIBLE
        buttonClear?.visibility = View.INVISIBLE
//        buttonLlm?.visibility = View.INVISIBLE
        buttonSwitch?.visibility = View.INVISIBLE
    }
    private val showButtons: () -> Unit = {
        buttonStartStop?.visibility = View.VISIBLE
        buttonClear?.visibility = View.VISIBLE
//        buttonLlm?.visibility = View.VISIBLE
        buttonSwitch?.visibility = View.VISIBLE

    }

    private val attrsReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            MyLog.i("broadcast", "onReceive")
            val attrs = intent?.getSerializableExtra("attributes") as? FloatingWindowAttributes
            attrs?.let {
                updateFloatingLayout(it)
                MyLog.i("broadcast", "onReceive--resize done")
            }

        }
    }

    init {

    }

    fun showFloatingWindow() {
        EasyFloat.with(context)
            .setShowPattern(ShowPattern.ALL_TIME)
            .setLayout(R.layout.floating_window) {view ->
                floatingWindowLayout = view.findViewById(R.id.floating_window)
                text1 = view.findViewById(R.id.floating_window_text)
                text1.movementMethod = ScrollingMovementMethod.getInstance()
                text1.textSize = 16f
                // 设置自定义触摸监听，实现Text拖动滚动
                text1.setOnTouchListener { v, event ->
                    v.parent.requestDisallowInterceptTouchEvent(true)
                    v.onTouchEvent(event)
                    true
                }
                textConcentration = view.findViewById(R.id.floating_window_llm_concentration)
                textConcentration.movementMethod = ScrollingMovementMethod.getInstance()
                textConcentration.textSize = 16f
                textConcentration.setOnTouchListener { v, event ->
                    v.parent.requestDisallowInterceptTouchEvent(true)
                    v.onTouchEvent(event)
                    true
                }

                textJson = view.findViewById(R.id.floating_window_llm_json)
                textJson.movementMethod = ScrollingMovementMethod.getInstance()
                textJson.textSize = 16f
                textJson.setOnTouchListener { v, event ->
                    v.parent.requestDisallowInterceptTouchEvent(true)
                    v.onTouchEvent(event)
                    true
                }

                backgroundLayout = view.findViewById(R.id.floating_window_text_background_layout)

                buttonStartStop = view.findViewById(R.id.btn_floating_start_stop_recording)
                buttonStartStop.setOnClickListener {
                    onStartStopClick()
                }

                buttonClear = view.findViewById(R.id.btn_floating_clear_context)
                buttonClear.setOnClickListener {
                    clearContext()
                }

//                buttonLlm = view.findViewById(R.id.btn_floating_llm_all)
//                buttonLlm.setOnClickListener {
//                    callLlm()
//                }

                buttonSwitch = view.findViewById(R.id.btn_floating_switch)
                buttonSwitch.setOnClickListener {
                    switchText()
                }

                emojiContainer = view.findViewById(R.id.floating_window_emoji_container)
            }
            .setDragEnable(true)
            .registerCallback {
                dismiss {
                    quit()
                }
                touchEvent { view, motionEvent ->
                    MyLog.d("floating", "touchEvent")
                    showButtons()
                    resetTimer()
                }
            }
            .show()
        handler = Handler(context.mainLooper)

        val filter = IntentFilter(context.getString(R.string.com_example_floating_resize_event))
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            context.registerReceiver(attrsReceiver, filter)
        else
            context.registerReceiver(attrsReceiver, filter, RECEIVER_EXPORTED)

        updateFloatingLayout(FloatingWindowAttributes(backgroundId = 1))
    }

    private fun resetTimer() {
        if(context.isCapture) {
            MyLog.d("ui", "resetTimer capturing")
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed(hideButtons, 5000L)
        }else{
            MyLog.d("ui", "resetTimer not capturing")
            handler.removeCallbacksAndMessages(null)
        }
    }

    private fun switchText() {
//        textType = (textType + 1) % 4
        textType = (textType % 3) + 1

        if (textType == 0) {
            text1.visibility = View.VISIBLE
            textConcentration.visibility = View.VISIBLE
            textJson.visibility = View.VISIBLE
        }
        else{
            text1.visibility = if(textType == 1) { View.VISIBLE } else {View.GONE}
            textConcentration.visibility = if(textType == 2) { View.VISIBLE } else {View.GONE}
            textJson.visibility = if(textType == 3) { View.VISIBLE } else {View.GONE}
        }

        buttonSwitch.text = "切换界面($textType/3)"
    }

    private fun callLlm() {
        MyLog.d("llm", "提取文本：" + wordContainer.getText())
        GlobalScope.launch {
            context.callLlm()
        }
    }

    private fun clearContext() {
        wordContainer.clear()
        text1.text = context.getString(R.string.transcriptionTextDefault)
        text1.scrollY = 0
        textConcentration.text = context.getString(R.string.concentrationTextDefault)
        textJson.text = context.getString(R.string.jsonTextDefault)

        context.clearContext()
    }

    fun newTranscription(message: TranscriptionMessage) {
        GlobalScope.launch(Dispatchers.Main) {
            // 保存当前滚动位置和原文本长度
            val oldScrollAmount = (text1.layout?.getLineTop(text1.lineCount) ?: 0) - text1.height   // 在调整 layout 时，有概率会变成 null，要加一个 ?.
            val oldTextLength = wordContainer.getText().length
            val isAtBottom = oldScrollAmount <= text1.scrollY

            // 更新文本
            wordContainer.addWords(
                message.segmentedData.getAbsolute(
                    context.startTime ?: LocalDateTime.MIN
                )
            )
            text1.text = wordContainer.getText()

            // find the amount we need to scroll.  This works by
            // asking the TextView's internal layout for the position
            // of the final line and then subtracting the TextView's height
            val lineTop = text1.layout.getLineTop(text1.lineCount)
            val height = text1.height
            val scrollAmount = text1.layout.getLineTop(text1.lineCount) -
                    (text1.height - text1.paddingTop - text1.paddingBottom)
            // if there is no need to scroll, scrollAmount will be <=0
//        if(text1.layout.getLineTop(text1.lineCount))
            val scrollY = text1.scrollY
            if (isAtBottom && scrollAmount > 0)
                text1.scrollTo(0, scrollAmount)
        }
    }

    // bug: 新的
    suspend fun newConcentration(concentration: String) {
        textConcentration.text = ""
        val stringBuilder = StringBuilder()
        for(slice in concentration.chunked(3)){
            stringBuilder.append(slice)
            textConcentration.text = stringBuilder.toString()
            delay(100)
        }
    }

    suspend fun newConcentration(concentration: Flow<String>): String{
        MyLog.i("llm", "开始接收流式请求")
        val sb = StringBuilder()
        concentration.collect {
            delay(350)
            sb.append(it)
            textConcentration?.text = sb.toString()
        }
        MyLog.i("llm", "Logic结束接收流式请求")

        return sb.toString()

    }

    fun newJson(s: String) {
        textJson.text = s
    }

    private fun onStartStopClick() {
        context.isCapture = !context.isCapture
        if(context.isCapture){
            repeatingTask.start()
            resetTimer()
            context.startAudioCapture()
            buttonStartStop.text = "结束捕获"
        } else {
            repeatingTask.stop()
            context.stopAudioCapture()
            buttonStartStop.text = "开始捕获"
        }
    }

    fun quit() {
        context.unregisterReceiver(attrsReceiver)
        context.deactivate()
    }

    fun updateFloatingLayout(attrs: FloatingWindowAttributes) {
        GlobalScope.launch(Dispatchers.Main) {
            attrs.width?.let { w ->
                MyLog.i("layout", "width: $w")

                val layoutParams = text1.layoutParams
                layoutParams.width = w
                text1.layoutParams = layoutParams
                textConcentration.layoutParams.width = w
                textJson.layoutParams.width = w

//            (text1.parent as? ViewGroup)?.requestLayout()
            }

            attrs.height?.let { h ->
                MyLog.i("layout", "height: $h")
                var layoutParams = text1.layoutParams
                layoutParams.height = h
                textJson.layoutParams = layoutParams


                text1.height = h

                layoutParams = textConcentration.layoutParams
                layoutParams.height = h
                textConcentration.layoutParams = layoutParams

                textJson.height = h
            }

            attrs.fontSize?.let { fs ->
                text1.textSize = fs
                textConcentration.textSize = fs
                textJson.textSize = fs
            }

            attrs.alpha?.let { a ->
                text1.alpha = a
                textConcentration.alpha = a
                textJson.alpha = a
            }

            attrs.backgroundId?.let { id ->
                val resourceId = when(id) {
                    1 -> R.drawable.bubble1
                    2 -> R.drawable.bubble2
                    3 -> R.drawable.bubble3
                    else -> R.drawable.bubble1
                }

                val padding = when(id){
                    1 -> 12
                    2 -> 12
                    3 -> 50
                    else -> 0
                }

                val paddingBottom = when(id) {
                    1-> 24
                    2-> 24
                    3-> 55
                    else -> 0
                }
                val paddingLeft = when(id) {
                    1-> 24
                    2-> 24
                    3-> 40
                    else -> 0
                }


                val paddingRight = when(id){
                    1 -> 10
                    2 -> 10
                    3 -> 58
                    else -> 0
                }

                val textColor = when(id) {
                    1-> context.getColor(R.color.white)
                    2-> context.getColor(R.color.speech_bubble_purple_text_color)
                    3-> context.getColor(R.color.black)
                    else-> context.getColor(R.color.white)
                }

                val actPadding = (padding * context.resources.displayMetrics.density).toInt()
                val actPaddingBottom = (paddingBottom * context.resources.displayMetrics.density).toInt()
                val actPaddingLeft = (paddingLeft * context.resources.displayMetrics.density).toInt()
                val actPaddingRight = (paddingRight * context.resources.displayMetrics.density).toInt()

                for(tv in arrayOf(text1, textConcentration, textJson)) {
                    tv.setBackgroundResource(resourceId)
                    tv.setPadding(actPaddingLeft, actPaddingRight, actPadding, actPaddingBottom)
                    MyLog.d("floating", "old color: ${tv.textColors} color: $textColor")
                    tv.setTextColor(textColor)
                }
            }
        }
    }

    private fun addEmoji(emoji: String, elapseInMills: Long){
        val initAlpha = 1.0f
        MyLog.d("floating", "Adding emoji")
        val textView = TextView(context).apply {
            text = emoji
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.INVISIBLE
            textSize = 30f
            alpha = initAlpha
            setPadding(10, 10, 10, 10)
        }

        emojiContainer.addView(textView)
        MyLog.d("floating", "Emoji $emoji added")

        textView.post {
            MyLog.d("floating", "Trying relocating emoji")
            MyLog.d("floating", "container layout: width=${emojiContainer.layoutParams.width}, height=${emojiContainer.layoutParams.height}")
            MyLog.d("floating", "container: width=${emojiContainer.width}, height=${emojiContainer.height}")
            MyLog.d("floating", "textView: width=${textView.width}, height=${textView.height}")

            val x = Random.nextInt(emojiContainer.width - textView.width)
            val y = Random.nextInt(emojiContainer.height - textView.height)
            textView.x = x.toFloat()
            textView.y = y.toFloat()

            textView.visibility = View.VISIBLE
            MyLog.d("floating", "Emoji $emoji is visible")

        }

        textView.postDelayed({
            val alpha = PropertyValuesHolder.ofFloat("alpha", initAlpha, 0.0f)
            val translateY = PropertyValuesHolder.ofFloat("translationY", textView.y, textView.y-textView.height.toFloat())
            ObjectAnimator.ofPropertyValuesHolder(textView, alpha, translateY).apply {
                duration = 200
                start()
            }
        }, elapseInMills)

        textView.postDelayed({
            emojiContainer.removeView(textView)
            MyLog.d("floating", "Emoji $emoji removed")
        }, elapseInMills + 1000)
    }

    suspend fun newEmojis(str: String) {
        val emojis = str.filterEmoji()
        withContext(Dispatchers.Main) {
            for (emoji in emojis) addEmoji(emoji, 20000L)
        }
    }

    private fun String.filterEmoji() : List<String> {
        return EmojiParser.extractEmojis(this)
    }
}

fun solveBold(input: String): SpannableString {
    val indexes = input.indexesOf("**")
//    int
    throw Exception()
}

data class FloatingWindowAttributes(
    val width: Int? = null,
    val height: Int? = null,
    val alpha: Float? = null,

    val fontSize: Float? = null,

    val backgroundId: Int? = null,
) : Serializable