package vizzletf.movietorr

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.io.IOException

class TorApiService {
    
    private val client = OkHttpClient()
    private val gson = Gson()
    private val baseUrl = "https://torapi-vizzletf.vercel.app"
    
    // Универсальная модель для всех трекеров
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
    
    // Модели для каждого трекера согласно Swagger
    data class RuTrackerItem(
        val Name: String,
        val Id: String,
        val Url: String,
        val Torrent: String,
        val Size: String,
        val Download_Count: String,
        val Checked: String,
        val Category: String,
        val Seeds: String,
        val Peers: String,
        val Date: String
    ) {
        fun toTorrentItem(): TorrentItem = TorrentItem(
            Id = Id,
            Name = Name,
            Size = Size,
            Seeds = Seeds,
            Peers = Peers,
            Date = Date,
            Url = Url,
            Torrent = Torrent,
            Category = Category
        )
    }
    
    data class KinozalItem(
        val Name: String,
        val Title: String,
        val Id: String,
        val Original_Name: String,
        val Year: String,
        val Language: String,
        val Format: String,
        val Url: String,
        val Torrent: String,
        val Size: String,
        val Comments: String,
        val Category: String,
        val Seeds: String,
        val Peers: String,
        val Date: String
    ) {
        fun toTorrentItem(): TorrentItem = TorrentItem(
            Id = Id,
            Name = Name,
            Size = Size,
            Seeds = Seeds,
            Peers = Peers,
            Date = Date,
            Url = Url,
            Torrent = Torrent,
            Category = Category
        )
    }
    
    data class RuTorItem(
        val Name: String,
        val Id: String,
        val Url: String,
        val Torrent: String,
        val Hash: String,
        val Size: String,
        val Comments: String,
        val Seeds: String,
        val Peers: String,
        val Date: String
    ) {
        fun toTorrentItem(): TorrentItem = TorrentItem(
            Id = Id,
            Name = Name,
            Size = Size,
            Seeds = Seeds,
            Peers = Peers,
            Date = Date,
            Url = Url,
            Torrent = Torrent,
            Hash = Hash
        )
    }
    
    data class NoNameClubItem(
        val Name: String,
        val Id: String,
        val Url: String,
        val Torrent: String,
        val Size: String,
        val Comments: String,
        val Category: String,
        val Seeds: String,
        val Peers: String,
        val Date: String
    ) {
        fun toTorrentItem(): TorrentItem = TorrentItem(
            Id = Id,
            Name = Name,
            Size = Size,
            Seeds = Seeds,
            Peers = Peers,
            Date = Date,
            Url = Url,
            Torrent = Torrent,
            Category = Category
        )
    }
    
    data class TorrentResponse(
        val RuTracker: List<RuTrackerItem>? = null,
        val Kinozal: List<KinozalItem>? = null,
        val RuTor: List<RuTorItem>? = null,
        val NoNameClub: List<NoNameClubItem>? = null
    ) {
        fun getRuTrackerList(): List<TorrentItem> = RuTracker?.map { it.toTorrentItem() } ?: emptyList()
        fun getKinozalList(): List<TorrentItem> = Kinozal?.map { it.toTorrentItem() } ?: emptyList()
        fun getRuTorList(): List<TorrentItem> = RuTor?.map { it.toTorrentItem() } ?: emptyList()
        fun getNoNameClubList(): List<TorrentItem> = NoNameClub?.map { it.toTorrentItem() } ?: emptyList()
    }
    
    data class MagnetResponse(
        val Magnet: String,
        val Hash: String
    )
    
    interface TorrentSearchCallback {
        fun onSuccess(response: TorrentResponse)
        fun onError(error: String)
    }
    
    interface MagnetCallback {
        fun onSuccess(magnet: String, hash: String)
        fun onError(error: String)
    }
    
    fun searchTorrents(query: String, callback: TorrentSearchCallback) {
        val url = "$baseUrl/api/search/title/all?query=$query"
        val request = Request.Builder()
            .url(url)
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError("Ошибка сети: ${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        callback.onError("Ошибка сервера: ${response.code}")
                        return
                    }
                    
                    try {
                        val responseBody = response.body?.string()
                        println("TorAPI Response: $responseBody")
                        
                        // Проверяем структуру ответа
                        if (responseBody?.startsWith("{") == true) {
                            try {
                                val torrentResponse = gson.fromJson(responseBody, TorrentResponse::class.java)
                                println("Успешно распарсили ответ от TorAPI")
                                callback.onSuccess(torrentResponse)
                            } catch (parseException: Exception) {
                                println("Ошибка парсинга JSON: ${parseException.message}")
                                println("Пробуем альтернативный парсинг для диагностики...")
                                
                                // Попробуем парсить как Map для диагностики
                                try {
                                    val type = object : TypeToken<Map<String, Any>>() {}.type
                                    val responseMap: Map<String, Any> = gson.fromJson(responseBody, type)
                                    println("Структура ответа: ${responseMap.keys}")
                                    
                                    // Создаем пустой ответ
                                    val emptyResponse = TorrentResponse()
                                    callback.onSuccess(emptyResponse)
                                } catch (mapException: Exception) {
                                    println("Не удалось распарсить даже как Map: ${mapException.message}")
                                    callback.onError("Ошибка парсинга ответа API: ${parseException.message}")
                                }
                            }
                        } else {
                            callback.onError("Неожиданный формат ответа от API")
                        }
                    } catch (e: Exception) {
                        callback.onError("Ошибка чтения ответа: ${e.message}")
                    }
                }
            }
        })
    }
    

    
    fun getMagnetLink(source: String, id: String, callback: MagnetCallback) {
        val url = "$baseUrl/api/search/id/${source.lowercase()}?query=$id"
        val request = Request.Builder()
            .url(url)
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError("Ошибка сети: ${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        callback.onError("Ошибка сервера: ${response.code}")
                        return
                    }
                    
                    try {
                        val responseBody = response.body?.string()
                        println("Magnet API Response: $responseBody")
                        
                        val type = object : TypeToken<List<MagnetResponse>>() {}.type
                        val magnetList: List<MagnetResponse> = gson.fromJson(responseBody, type)
                        
                        if (magnetList.isNotEmpty()) {
                            val magnetData = magnetList[0]
                            callback.onSuccess(magnetData.Magnet, magnetData.Hash)
                        } else {
                            callback.onError("Магнитная ссылка не найдена")
                        }
                    } catch (e: Exception) {
                        callback.onError("Ошибка парсинга: ${e.message}")
                    }
                }
            }
        })
    }
} 