package com.example.audiocapturer

import com.example.audiocapturer.utils.SegmentedData
import org.json.JSONObject

open class TranscriptionResp{

     companion object {
         fun Create(message: String) : TranscriptionResp {
             val result = JSONObject(message)
             if (result.optString("action") != "result") {
                 return TranscriptionResp()
             }

             return TranscriptionMessage(SegmentedData.create(result.getString("data")))
         }
    }
}

data class TranscriptionMessage(val segmentedData: SegmentedData) : TranscriptionResp() {

}