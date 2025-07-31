package vizzletf.movietorr

import com.google.gson.Gson
import okhttp3.*
import java.io.IOException

class TorrentService {
    
    private val client = OkHttpClient()
    private val gson = Gson()
    private val baseUrl = "https://torapi.vercel.app"
    
    data class TorrentItem(
        val Id: String,
        val Name: String,
        val Size: String,
        val Seeds: String,
        val Peers: String,
        val Date: String,
        val Url: String,
        val Torrent: String,
        val Category: String? = null,
        val Hash: String? = null
    )
    
    fun searchTorrents(query: String, callback: (List<TorrentItem>?, String?) -> Unit) {
        val request = Request.Builder()
            .url("$baseUrl/search?query=${java.net.URLEncoder.encode(query, "UTF-8")}")
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null, e.message)
            }
            
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    try {
                        val torrentItems = gson.fromJson(responseBody, Array<TorrentItem>::class.java).toList()
                        callback(torrentItems, null)
                    } catch (e: Exception) {
                        callback(null, "Ошибка парсинга данных: ${e.message}")
                    }
                } else {
                    callback(null, "Ошибка запроса: ${response.code}")
                }
            }
        })
    }
} 