package com.librekinopoisk.android

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class SiteConfig(
    val id: String,
    val name: String,
    val url: String,
    val isEnabled: Boolean = true
)

object SiteSettingsManager {
    private const val PREF_NAME = "SiteSettings"
    private const val KEY_ENABLED_SITES = "enabled_sites"
    private const val KEY_CUSTOM_SITES = "custom_sites"
    
    private val gson = Gson()
    
    // Стандартные сайты
    val defaultSites = listOf(
        SiteConfig("freepik", "Freepik", "https://www.freepik.com/videos", true)
    )
    
    fun getEnabledSites(context: Context): List<SiteConfig> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val removedDefaultSites = prefs.getStringSet("removed_default_sites", setOf()) ?: setOf()
        
        val customSites = getCustomSites(context)
        val availableDefaultSites = defaultSites.filter { it.id !in removedDefaultSites }
        val allSites = availableDefaultSites + customSites
        
        return allSites
    }
    
    fun getCustomSites(context: Context): List<SiteConfig> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val customSitesJson = prefs.getString(KEY_CUSTOM_SITES, "[]")
        val type = object : TypeToken<List<SiteConfig>>() {}.type
        return gson.fromJson(customSitesJson, type) ?: emptyList()
    }
    
    fun addCustomSite(context: Context, name: String, url: String): Boolean {
        if (name.isBlank() || url.isBlank()) return false
        
        val customSites = getCustomSites(context).toMutableList()
        val newSite = SiteConfig(
            id = "custom_${System.currentTimeMillis()}",
            name = name.trim(),
            url = url.trim()
        )
        
        customSites.add(newSite)
        saveCustomSites(context, customSites)
        
        return true
    }
    
    fun removeCustomSite(context: Context, siteId: String) {
        val customSites = getCustomSites(context).filter { it.id != siteId }
        saveCustomSites(context, customSites)
    }
    
    fun removeDefaultSite(context: Context, siteId: String) {
        // Для дефолтных сайтов просто сохраняем информацию об удалении
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val removedDefaultSites = prefs.getStringSet("removed_default_sites", setOf())?.toMutableSet() ?: mutableSetOf()
        removedDefaultSites.add(siteId)
        prefs.edit().putStringSet("removed_default_sites", removedDefaultSites).apply()
    }
    
    private fun saveCustomSites(context: Context, sites: List<SiteConfig>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val sitesJson = gson.toJson(sites)
        prefs.edit().putString(KEY_CUSTOM_SITES, sitesJson).apply()
    }
    
    fun getAllSites(context: Context): List<SiteConfig> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val removedDefaultSites = prefs.getStringSet("removed_default_sites", setOf()) ?: setOf()
        
        val customSites = getCustomSites(context)
        val availableDefaultSites = defaultSites.filter { it.id !in removedDefaultSites }
        val allSites = availableDefaultSites + customSites
        
        return allSites
    }
    
} 