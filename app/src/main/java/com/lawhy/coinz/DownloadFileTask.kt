package com.lawhy.coinz

import android.content.Intent
import android.os.AsyncTask
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


interface DownloadCompleteListener {
    // Once the download complete, shift from curActivity to the nextActivity
    fun downloadComplete(result: String, intent: Intent)
}

class DownloadFileTask(private val caller: DownloadCompleteListener,
                       private val intent: Intent) : AsyncTask<String, Void, String>() {

    override fun doInBackground(vararg urls: String): String = try{
        loadFileFromNetwork(urls[0])
    } catch (e: IOException){
        "Unable to load the content. Please check your network connection."
    }

    private fun loadFileFromNetwork(urlString: String): String {
        val stream : InputStream = downloadUrl(urlString)
        // read input from the stream, read the result as a string
        val result = stream.bufferedReader().use { it.readText() }
        stream.close()
        return result
    }

    // Given a string representation of a URL, sets up a connection and gets an input stream
    @Throws(IOException::class)
    fun downloadUrl(urlString: String): InputStream {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection

        conn.readTimeout = 10000
        conn.connectTimeout = 15000
        conn.requestMethod = "GET"
        conn.doInput = true
        conn.connect()
        return conn.inputStream
    }

    override fun onPostExecute(result: String) {
        super.onPostExecute(result)
        caller.downloadComplete(result, intent)
    }
}