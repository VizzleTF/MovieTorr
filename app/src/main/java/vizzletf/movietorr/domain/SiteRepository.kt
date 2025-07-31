package vizzletf.movietorr.domain

import vizzletf.movietorr.SiteConfig
import vizzletf.movietorr.data.PreferencesRepository

class SiteRepository(private val preferencesRepository: PreferencesRepository) {
    
    private val defaultSites = listOf(
        SiteConfig("freepik", "Freepik", "https://www.freepik.com/videos", true)
    )
    
    fun getEnabledSites(): List<SiteConfig> {
        val removed = preferencesRepository.getRemovedDefaultSites()
        val customSites = preferencesRepository.getCustomSites()
        val availableDefaultSites = defaultSites.filter { it.id !in removed }
        return availableDefaultSites + customSites
    }
    
    fun getCustomSites(): List<SiteConfig> = preferencesRepository.getCustomSites()
    
    fun addCustomSite(name: String, url: String): Boolean {
        if (name.isBlank() || url.isBlank()) return false
        
        val customSites = getCustomSites().toMutableList()
        val newSite = SiteConfig(
            id = "custom_${System.currentTimeMillis()}",
            name = name.trim(),
            url = url.trim()
        )
        customSites.add(newSite)
        preferencesRepository.saveCustomSites(customSites)
        return true
    }
    
    fun removeCustomSite(siteId: String) {
        val customSites = getCustomSites().filter { it.id != siteId }
        preferencesRepository.saveCustomSites(customSites)
    }
    
    fun updateCustomSite(siteId: String, name: String, url: String): Boolean {
        if (name.isBlank() || url.isBlank()) return false
        
        val customSites = getCustomSites().toMutableList()
        val siteIndex = customSites.indexOfFirst { it.id == siteId }
        if (siteIndex == -1) return false
        
        customSites[siteIndex] = SiteConfig(
            id = siteId,
            name = name.trim(),
            url = url.trim()
        )
        preferencesRepository.saveCustomSites(customSites)
        return true
    }
    
    fun removeDefaultSite(siteId: String) {
        preferencesRepository.addRemovedDefaultSite(siteId)
    }
    
    fun getLastSource(): String = preferencesRepository.getLastSource()
    
    fun saveLastSource(source: String) {
        preferencesRepository.setLastSource(source)
    }
} 