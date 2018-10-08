package com.lawhy.coinz
import android.util.Log

interface DownloadCompleteListener {
    fun downloadComplete(result: String)
}

object DownloadCompleteRunner : DownloadCompleteListener {
    var result : String? = null
    override fun downloadComplete(result: String) {
        this.result = result
        Log.i("DownloadTask", "Completed $result!")
    }
}