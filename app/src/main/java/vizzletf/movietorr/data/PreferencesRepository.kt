package vizzletf.movietorr.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import vizzletf.movietorr.SiteConfig

class PreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val PREF_NAME = "MovieTorrPrefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_CUSTOM_SITES = "custom_sites"
        private const val KEY_LAST_SOURCE = "last_source"
        private const val KEY_REMOVED_DEFAULT_SITES = "removed_default_sites"
        
        // Новые ключи для фильтров
        private const val KEY_ENABLED_TRACKERS = "enabled_trackers"
        private const val KEY_SIZE_FILTER_MIN = "size_filter_min"
        private const val KEY_SIZE_FILTER_MAX = "size_filter_max"
        private const val KEY_SIZE_FILTER_UNIT = "size_filter_unit"
        private const val KEY_SEARCH_MODE = "search_mode"
        private const val KEY_MIN_SEEDS = "min_seeds"
    }
    
    fun getThemeMode(): Int = prefs.getInt(KEY_THEME_MODE, 0)
    
    fun setThemeMode(themeMode: Int) {
        prefs.edit { putInt(KEY_THEME_MODE, themeMode) }
    }
    
    fun getCustomSites(): List<SiteConfig> {
        val customSitesJson = prefs.getString(KEY_CUSTOM_SITES, "[]") ?: "[]"
        val type = object : TypeToken<List<SiteConfig>>() {}.type
        return gson.fromJson(customSitesJson, type) ?: emptyList()
    }
    
    fun saveCustomSites(sites: List<SiteConfig>) {
        val sitesJson = gson.toJson(sites)
        prefs.edit { putString(KEY_CUSTOM_SITES, sitesJson) }
    }
    
    fun getLastSource(): String = prefs.getString(KEY_LAST_SOURCE, "freepik") ?: "freepik"
    
    fun setLastSource(source: String) {
        prefs.edit { putString(KEY_LAST_SOURCE, source) }
    }
    
    fun getRemovedDefaultSites(): Set<String> = prefs.getStringSet(KEY_REMOVED_DEFAULT_SITES, setOf()) ?: setOf()
    
    fun addRemovedDefaultSite(siteId: String) {
        val removed = getRemovedDefaultSites().toMutableSet()
        removed.add(siteId)
        prefs.edit { putStringSet(KEY_REMOVED_DEFAULT_SITES, removed) }
    }
    
    // Новые методы для фильтров
    fun getEnabledTrackers(): Set<String> {
        val defaultTrackers = setOf("RuTracker", "RuTor", "NoNameClub", "Kinozal")
        return prefs.getStringSet(KEY_ENABLED_TRACKERS, defaultTrackers) ?: defaultTrackers
    }
    
    fun setEnabledTrackers(trackers: Set<String>) {
        prefs.edit { putStringSet(KEY_ENABLED_TRACKERS, trackers) }
    }
    
    fun getSizeFilterMin(): Int = prefs.getInt(KEY_SIZE_FILTER_MIN, 0)
    
    fun setSizeFilterMin(value: Int) {
        prefs.edit { putInt(KEY_SIZE_FILTER_MIN, value) }
    }
    
    fun getSizeFilterMax(): Int = prefs.getInt(KEY_SIZE_FILTER_MAX, 0)
    
    fun setSizeFilterMax(value: Int) {
        prefs.edit { putInt(KEY_SIZE_FILTER_MAX, value) }
    }
    
    fun getSizeFilterUnit(): String = prefs.getString(KEY_SIZE_FILTER_UNIT, "MB") ?: "MB"
    
    fun setSizeFilterUnit(unit: String) {
        prefs.edit { putString(KEY_SIZE_FILTER_UNIT, unit) }
    }
    
    fun getSearchMode(): Int = prefs.getInt(KEY_SEARCH_MODE, 0)
    
    fun setSearchMode(mode: Int) {
        prefs.edit { putInt(KEY_SEARCH_MODE, mode) }
    }
    
    fun getMinSeeds(): Int = prefs.getInt(KEY_MIN_SEEDS, 0)
    
    fun setMinSeeds(seeds: Int) {
        prefs.edit { putInt(KEY_MIN_SEEDS, seeds) }
    }
} 