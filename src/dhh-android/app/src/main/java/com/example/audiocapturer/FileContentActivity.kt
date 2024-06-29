package com.example.audiocapturer

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.audiocapturer.R
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException

class FileContentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.file_content_activity)

        val textViewFileContent: TextView = findViewById(R.id.textview_file_content)

        val fileName = intent.getStringExtra("fileName")
        val file = File(filesDir, "history/$fileName")

        val text = StringBuilder()

        try {
            val br = BufferedReader(FileReader(file))
            br.useLines { lines ->
                lines.forEach { line ->
                    text.append(line).append('\n')
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        textViewFileContent.text = text.toString()
    }
}
