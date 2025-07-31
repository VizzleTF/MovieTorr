package vizzletf.movietorr

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SiteManager {
    private const val PREF_NAME = "SiteSettings"
    private const val KEY_CUSTOM_SITES = "custom_sites"
    private const val KEY_LAST_SOURCE = "last_source"
    private const val KEY_REMOVED_DEFAULT_SITES = "removed_default_sites"
    
    private val gson = Gson()
    
    val defaultSites = listOf(
        SiteConfig("freepik", "Freepik", "https://www.freepik.com/videos", true)
    )
    
    fun getEnabledSites(context: Context): List<SiteConfig> {
        val removed = getRemovedDefaultSites(context)
        val customSites = getCustomSites(context)
        val availableDefaultSites = defaultSites.filter { it.id !in removed }
        return availableDefaultSites + customSites
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
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val removed = getRemovedDefaultSites(context).toMutableSet()
        removed.add(siteId)
        prefs.edit().putStringSet(KEY_REMOVED_DEFAULT_SITES, removed).apply()
    }
    
    private fun getRemovedDefaultSites(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_REMOVED_DEFAULT_SITES, setOf()) ?: setOf()
    }
    
    private fun saveCustomSites(context: Context, sites: List<SiteConfig>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val sitesJson = gson.toJson(sites)
        prefs.edit().putString(KEY_CUSTOM_SITES, sitesJson).apply()
    }
    
    fun saveLastSource(context: Context, source: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_SOURCE, source).apply()
    }
    
    fun getLastSource(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_SOURCE, "freepik") ?: "freepik"
    }
} 