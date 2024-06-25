package com.example.audiocapturer.utils

import android.util.Log
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class SegmentedData(
    val segId: Int,
    val bg: Int,
    val ed: Int,
    val words: Array<WordInInt>
) {
    companion object{
        fun create(jsonString: String): SegmentedData {
            val jsonObject = JSONObject(jsonString)

            val segId = jsonObject.getInt("seg_id")
            val cn = jsonObject.getJSONObject("cn")
            val st = cn.getJSONObject("st")
            val rtArray = st.getJSONArray("rt")
            val rt = rtArray.getJSONObject(0)
            val wsArray = rt.getJSONArray("ws")

            val bg = st.getInt("bg")
            val ed = st.getInt("ed")
            val words = mutableListOf<WordInInt>()

            for (i in 0 until wsArray.length()) {
                val ws = wsArray.getJSONObject(i)
                val cwArray = ws.getJSONArray("cw")
                val cw = cwArray.getJSONObject(0)
                val text = cw.getString("w")
                val wb = cw.getInt("wb")
                val we = cw.getInt("we")
                words.add(WordInInt(text, wb, we))
            }

            return SegmentedData(segId, bg, ed, words.toTypedArray())
        }
    }

    fun getAbsolute(timeBase: LocalDateTime): List<WordWithDateTime> {
        return words.map { word ->
            WordWithDateTime(
                text = word.text,
                wb = timeBase.plus(bg + word.wb * 10L, ChronoUnit.MILLIS),
                we = timeBase.plus(bg + word.we * 10L, ChronoUnit.MILLIS),
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SegmentedData

        if (segId != other.segId) return false
        if (bg != other.bg) return false
        if (ed != other.ed) return false
        if (!words.contentEquals(other.words)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = segId
        result = 31 * result + bg.hashCode()
        result = 31 * result + ed.hashCode()
        result = 31 * result + words.contentHashCode()
        return result
    }
}

data class WordInInt(
    val text: String,
    val wb: Int,
    val we: Int,
)

data class WordWithDateTime(
    val text: String,
    val wb: LocalDateTime,
    val we: LocalDateTime
)


class WordContainer {
    private val words: MutableList<WordWithDateTime> = mutableListOf()
    private val textRepresentation: StringBuilder = StringBuilder()
    private val lock = Any()

    /**
     * 添加新的 Word 列表到容器中。
     * @param newWords 新的 Word 列表
     * @return 被删除的 Word 数量
     */
    fun addWords(newWords: List<WordWithDateTime>): Int {
        if (newWords.isEmpty()) return 0

        synchronized(lock) {
            // 按 wb 升序排序新添加的 Word 列表
            val sortedNewWords = newWords.sortedBy { it.wb }

            // 找到新列表中 wb 最小的值
            val minWbNewWord = sortedNewWords.first().wb

            // 删除容器中所有 wb 大于等于 minWbNewWord 的 Word
            val beginRemove = words.indexOfFirst { it.wb >= minWbNewWord }
            val removeChars = if (beginRemove >= 0) {
                val n = words.drop(beginRemove).sumOf { it.text.length }
                words.subList(beginRemove, words.size).clear()
                n
            } else {
                0
            }

            textRepresentation.delete(
                textRepresentation.length - removeChars,
                textRepresentation.length
            )

            // 添加新的 Word 到容器
            words.addAll(sortedNewWords)

            // 添加新词到 StringBuilder
            sortedNewWords.forEach { textRepresentation.append(it.text) }

            return removeChars
        }
    }

    /**
     * 返回当前容器的文本表示。
     */
    fun getText(): String {
        return textRepresentation.toString()
    }

    fun getTextSince(time: LocalDateTime): String {
        val ret = words.dropWhile { it.wb < time }.joinToString(separator = "") { it.text }
        MyLog.i("word", "getTextSince: $time -> $ret")
        return ret
    }

    fun clear() {
        synchronized(lock) {
            textRepresentation.clear()
            words.clear()
        }
    }
}
