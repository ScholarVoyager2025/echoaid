package com.example.audiocapturer

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.example.audiocapturer.utils.MyLog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history_activity)

        val listViewHistory: ListView = findViewById(R.id.listview_history)

        val historyDir = File(filesDir, "history")
        if (!historyDir.exists()) {
            historyDir.mkdir()
        }

        val txtFiles = historyDir.listFiles { dir, name -> name.endsWith(".txt") }

        if (txtFiles != null) {
            txtFiles.sortByDescending { it.lastModified() }

            val isoSdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            isoSdf.timeZone = TimeZone.getTimeZone("UTC")
            val sdf = SimpleDateFormat("yyyy'年'MM'月'dd'日'HH:mm:ss", Locale.getDefault())
            val formattedFileNames = txtFiles.map { file ->
                val timestamp = file.name.removeSuffix(".txt")
                val date = isoSdf.parse(timestamp)
                val formattedDate = sdf.format(date!!)
                formattedDate
            }

            val adapter = ArrayAdapter(this,
                android.R.layout.simple_list_item_1, formattedFileNames)
            listViewHistory.adapter = adapter

            listViewHistory.setOnItemClickListener { parent, view, position, id ->
                val fileName = txtFiles[position].name
                val intent = Intent(this, FileContentActivity::class.java)
                intent.putExtra("fileName", fileName)
                MyLog.i("history", "Open file $fileName")
                startActivity(intent)
            }
        }
    }
}
