package com.movietorr.android

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

data class SearchHistoryItem(
    val query: String,
    val date: String
)

object SearchHistoryManager {
    
    private const val PREF_NAME = "search_history"
    private const val KEY_HISTORY = "history"
    private const val MAX_HISTORY_SIZE = 50
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    fun addToHistory(context: Context, query: String) {
        val history = getHistory(context).toMutableList()
        
        // Удаляем дубликаты
        history.removeAll { it.query == query }
        
        // Добавляем новый запрос в начало
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val newItem = SearchHistoryItem(query, dateFormat.format(Date()))
        history.add(0, newItem)
        
        // Ограничиваем размер истории
        if (history.size > MAX_HISTORY_SIZE) {
            history.removeAt(history.size - 1)
        }
        
        saveHistory(context, history)
    }
    
    fun getHistory(context: Context): List<SearchHistoryItem> {
        val prefs = getPreferences(context)
        val json = prefs.getString(KEY_HISTORY, "[]")
        
        return try {
            val type = object : TypeToken<List<SearchHistoryItem>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun clearHistory(context: Context) {
        saveHistory(context, emptyList())
    }
    
    private fun saveHistory(context: Context, history: List<SearchHistoryItem>) {
        val prefs = getPreferences(context)
        val json = Gson().toJson(history)
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }
} 