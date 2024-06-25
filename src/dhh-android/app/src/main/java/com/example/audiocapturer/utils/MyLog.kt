package com.example.audiocapturer.utils

import android.util.Log
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MyLog {
    private var logWriter: FileWriter? = null

    fun setLogWriter(writer: FileWriter) {
        logWriter = writer
    }

    private fun writeLogToFile(tag: String, message: String, level: String, e: Exception? = null) {
        if (logWriter == null) {
            return
        }
        try {
            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val logMessage = "$timeStamp - $level - $tag: $message\n"
            logWriter?.append(logMessage)
            e?.let { logWriter?.append(e.stackTraceToString()) }
            logWriter?.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        writeLogToFile(tag, message, "DEBG")
    }

    fun e(tag: String, message: String) {
        Log.e(tag, message)
        writeLogToFile(tag, message, "EROR")
    }


    fun e(tag: String, message: String, e: Exception) {
        Log.e(tag, message, e)
        writeLogToFile(tag, message, "EROR", e)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        writeLogToFile(tag, message, "INFO")
    }

}