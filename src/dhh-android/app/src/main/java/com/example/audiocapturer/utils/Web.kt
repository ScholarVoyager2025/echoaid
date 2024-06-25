package com.example.audiocapturer.utils

import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec



//private fun sendRequest() {
//    val webSocket: WebSocket
//    try {
//        webSocket = httpClient.newWebSocket(wsRequest, listener)
//    } catch(e : Exception){
//        e.printStackTrace()
//        return
//    }
//    try {
//        val audioFile: File
//        FileInputStream(audioFile).use { inputStream ->
//            val buffer = ByteArray(1280)
//            var bytesRead: Int
//
//            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
//                if (bytesRead > 0) {
//                    webSocket.send(buffer.toByteString(0, bytesRead))
//                }
//                delay(40)
//            }
//            webSocket.send("{\"end\": true}")
//        }
//    } catch (e: IOException) {
//        e.printStackTrace()
//    } catch (e: Exception) {
//        e.printStackTrace()
//        throw e
//    }
//}

fun getWsUrl(): String {
    val appId = "*"
    val apiKey = "*"
    val baseUrl = "wss://rtasr.xfyun.cn/v1/ws"
    val ts = (System.currentTimeMillis() / 1000).toString()
    val md5 = MessageDigest.getInstance("MD5").digest((appId + ts).toByteArray()).joinToString("") { "%02x".format(it) }
    val signa = Base64.encodeToString(HmacSHA1(apiKey.toByteArray(), md5.toByteArray()), Base64.NO_WRAP)
    return "$baseUrl?appid=$appId&ts=$ts&signa=${Uri.encode(signa)}"
}

private fun HmacSHA1(key: ByteArray, data: ByteArray): ByteArray {
    val mac = Mac.getInstance("HmacSHA1")
    val secretKeySpec = SecretKeySpec(key, "HmacSHA1")
    mac.init(secretKeySpec)
    return mac.doFinal(data)
}
//
//private fun createWebSocketListener(): WebSocketListener {
//    return object : WebSocketListener() {
//        override fun onOpen(webSocket: WebSocket, response: Response) {
////            runOnUiThread { Toast.makeText(this@MainActivity, "WebSocket Opened", Toast.LENGTH_SHORT).show() }
//        }
//
//        override fun onMessage(webSocket: WebSocket, text: String) {
//            val result = JSONObject(text)
//            if (result.optString("action") == "result") {
//                val data = result.optString("data")
//                val dataJson = JSONObject(data)
//                val segId = dataJson.optInt("seg_id")
//                val cn = dataJson.optJSONObject("cn")
//                val st = cn?.optJSONObject("st")
//                val rtArray = st?.optJSONArray("rt")
//
//                if (rtArray != null) {
//                    for (i in 0 until rtArray.length()) {
//                        val rtObject = rtArray.getJSONObject(i)
//                        val wsArray = rtObject.optJSONArray("ws")
//
//                        val sb = StringBuilder()
//                        if (wsArray != null) {
//                            for (j in 0 until wsArray.length()) {
//                                val wsObject = wsArray.getJSONObject(j)
//                                val cwArray = wsObject.optJSONArray("cw")
//
//                                if (cwArray != null) {
//                                    for (k in 0 until cwArray.length()) {
//                                        val cwObject = cwArray.getJSONObject(k)
//                                        val word = cwObject.optString("w")
//                                        sb.append(word)
//                                    }
//                                }
//                            }
//                        }
//
//                        val finalText = "$segId: [\"${sb.toString()}\"]"
////                        runOnUiThread {
////                            transcriptionTextView.text = finalText
////                        }
//                    }
//                }
//            }
//        }
//
//        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
//            // Not used
//        }
//
//        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
////            runOnUiThread { Toast.makeText(this@MainActivity, "WebSocket Closed", Toast.LENGTH_SHORT).show() }
//        }
//
//        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
////            runOnUiThread { Toast.makeText(this@MainActivity, "WebSocket Failure: ${t.message}", Toast.LENGTH_SHORT).show() }
//        }
//    }
//}