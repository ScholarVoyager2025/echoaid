package com.example.audiocapturer.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.Reader
import java.time.Duration

class Ernie {
    var accessToken: String
    var httpClient: OkHttpClient = OkHttpClient.Builder().readTimeout(Duration.ZERO).build()

    companion object {
        val clientId = "*"   
        val secretKey = "*"
        val apiKey = "*"

        const val concentrationSystem =
            """能力与角色:你是一个直播带货内容处理助手，帮助听障人士客户了解商品信息。
背景信息:你的原始输入是直播带货主播的语音转文字内容。你的任务是接收并浓缩输入的录音转文字内容。注意，语音转录内容可能有重复、遗漏、识别错误的情况，特别是靠近末尾10秒长度的字符，因为它尚未在语音模型纠正。你需要仔细分析上下文，理解主播的信息。如果无法理解，请忽略这些信息。

指令:你的任务是接收并浓缩输入的录音转文字内容，输出不超过50字。提取最关键的信息。主播会有重复对话，这些应该忽略。仅提取文件中的信息，不需要扩写。
输出风格:请你以主播的口吻输出。

下面是输入："""

        const val emojiSystem = """下面是相关的Emoji。找到和主播内容相关的Emoji。
👍：主播特别推荐该产品。
👎：主播不推荐该产品。
⭐：用于表示产品评级，可以根据星级数量显示产品质量。
⏰：表示有较短限时（5分钟内）的活动（如抢购、抽奖）正在进行。
⌛：强调时间紧迫，即将结束。
✨：用于强调产品的特殊功能或亮点。
🔍：表示主播正在详细展示产品细节。                                                                             
📦：表示主播正在介绍新产品。
🆕：强调产品是新上市的。
💲：主播正在介绍价格。
💸：表示有促销政策。
🔖：表示有优惠券或折扣可用。
🏷️：显示具体的折扣价。
👋：表示直播即将结束，主播正在致谢。
🙏：表示感谢观众参与。

输出时，列举所有符合要求的 Emoji。有强调时，可以输出复数个Emoji。解释理由时，直接解释，不输出 Emoji。同时，不需要总结。
"""

        const val jsonSystem = """你是一个直播带货内容处理助手，帮助听障人士客户了解商品信息。要求如下：
                0. 你的原始输入是经过总结的直播带货主播的语音转文字内容。你的输入会包含最多10条内容，按时间升序排序，每条间隔30秒。
                1. 你的任务是接收并提取关键词。以以下格式输出（未提及的部分，请用null代替）：
                
                
                商品: ...
                类别: ...
                促销政策: ...
                是否包邮: ...
                7天无理由退货: ...
                价格: ...
                售后服务: ...
                产品介绍: ...
                使用体验: ...
                使用说明书: ...
                
                
                2. 注意，你必须严格按格式输出。不需要任何解释，禁止输出头尾的'```'。一旦格式解析错误，你会被立刻杀死。
                3. 语音转录内容可能有重复、遗漏、识别错误的情况，特别是靠近末尾10秒长度的字符，因为它尚未在语音模型纠正。你需要仔细分析上下文，理解主播的信息。
                4. 提取最关键的信息。主播会有很多故事、语气词、无效对话，这些应该忽略。
                5. 如果有实用信息，你需要以主播的视角浓缩文字。
                6. 浓缩的文字一段一句，语言简练，只保留最关键信息。
                7. 记住，输出我要求的，不需要输出 '```'。否则你会杀死的。这无法解析。
                开始处理。"""
    }

    init {
        accessToken = requestAccessToken() ?: throw Exception("获取 Access Token 失败")
    }

    private fun requestAccessToken(): String? {
        val url =
            "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=$apiKey&client_secret=$secretKey"
        val request = Request.Builder()
            .url(url)
            .post("".toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: return null
            val jsonData = JSONObject(body)
            jsonData.getString("access_token")
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * @param systemType 提示词类型。1: 浓缩，2: json
     */
    suspend fun execute(systemType: Int, message: String = "你好"): String? {
//        MyLog.d("llm", "llm 输入：$message")
        val url =
            "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions?access_token=$accessToken"
        val jsonObject = JSONObject()
            .put("role", "user")
            .put("content", message)
        val jsonArray = JSONArray()
        jsonArray.put(jsonObject)
        val system = when (systemType) {
            0 -> ""
            1 -> concentrationSystem
            2 -> jsonSystem
            else -> {
                throw Exception("不合法的 systemType")
            }
        }
        val messagesObject = JSONObject().put("messages", jsonArray).put("system", system)
        val requestBody =
            messagesObject.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        val resp = withContext(Dispatchers.IO) {
            try {
                val response = httpClient.newCall(request).execute()
//                return@withContext response.body?.string()
                val body = response.body?.string()
                if (body == null) {
                    MyLog.e("llm", "取到空响应，参数($systemType, $message)")
                    return@withContext null
                }
                val resultString = JSONObject(body).optString("result")
                if (resultString == null)
                    MyLog.e("llm", "缺少 result: $resultString")
                else if (resultString == "null")
                    MyLog.e("llm", "缺少 result: \"null\"")

                return@withContext resultString
            } catch (e: IOException) {
                e.printStackTrace()
                throw e
            }
        }
        return resp
    }


    /**
     * @param systemType 提示词类型。1: 浓缩，2: json
     */
    suspend fun execute(messages: Array<String>, topP: Float? = null): String? {
        MyLog.d("llm", "llm 输入：${messages.contentToString()}")
        val url =       // 文心4.0
            "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/ernie-4.0-8k-latest?access_token=$accessToken"

        if(messages.size % 2 != 1)
            throw Exception("必须是轮流提问")

        val jsonArray = JSONArray()
        for(i in messages.indices) {
            val jsonObject = JSONObject()
                .put("role", if(i%2==0){"user"}else{"assistant"})
                .put("content", messages[i])
            jsonArray.put(jsonObject)
        }

        val messagesObject = JSONObject().put("messages", jsonArray).put("system", concentrationSystem)
//        if(topP != null) messagesObject.put("top_p", topP.toString())
        val requestBody =
            messagesObject.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        val resp = withContext(Dispatchers.IO) {
            try {
                val response = httpClient.newCall(request).execute()    // emoji response
//                return@withContext response.body?.string()
                val body = response.body?.string()
                if (body == null) {
                    MyLog.e("llm", "取到空响应")
                    return@withContext null
                }
                // {"error_code":336003,"error_msg":"message content can not be empty","id":"as-ig62ngucpj"}
                val bodyJson = JSONObject(body)
                val error_msg = bodyJson.optString("error_msg")
                if(error_msg != "") {
                    MyLog.e("llm", "上游返回错误: $error_msg, code: ${bodyJson.optInt("error_code")}")
                    return@withContext null
                }
                val resultString = bodyJson.optString("result")
                if (resultString == null)
                    MyLog.e("llm", "缺少 result: $resultString")
                else if (resultString == "null")
                    MyLog.e("llm", "缺少 result: \"null\"")

                return@withContext resultString
            } catch (e: IOException) {
                e.printStackTrace()
                throw e
            }
        }
        return resp
    }
    suspend fun executeStream(systemType: Int, message: String = "你好"): Flow<String> = flow {
        MyLog.d("llm", "流式 llm 输入：$message")
        val url =
            "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions?access_token=$accessToken"

        val jsonObject = JSONObject()
            .put("role", "user")
            .put("content", message)
            .put("stream", true)
        val jsonArray = JSONArray()
        jsonArray.put(jsonObject)
        val system = when (systemType) {
            1 -> concentrationSystem
            2 -> jsonSystem
            else -> {
                throw Exception("不合法的 systemType")
            }
        }
        val messagesObject = JSONObject().put("messages", jsonArray).put("system", system)
        val requestBody = messagesObject.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val resp: Reader? = withContext(Dispatchers.IO) {
            try {
                val response = httpClient.newCall(request).execute()
                if (response.body == null) {
                    MyLog.e("llm", "取到空响应，参数($systemType, $message)")
                    return@withContext null
                }
                return@withContext response.body!!.charStream()
            } catch (e: IOException) {
                e.printStackTrace()
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }

        resp?.useLines { lines ->
            for (line in lines) {
                emit(JSONObject(line.substringAfter("data: ")).optString("result"))
            }
        }
        MyLog.d("llm", "流式响应返回了")

    }


}