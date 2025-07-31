package vizzletf.movietorr

object SiteManager {
    // Делегируем к новому Repository для обратной совместимости
    private var siteRepository: vizzletf.movietorr.domain.SiteRepository? = null
    
    fun initialize(context: android.content.Context) {
        val preferencesRepository = vizzletf.movietorr.data.PreferencesRepository(context)
        siteRepository = vizzletf.movietorr.domain.SiteRepository(preferencesRepository)
    }
    
    fun getEnabledSites(context: android.content.Context): List<SiteConfig> {
        ensureInitialized(context)
        return siteRepository?.getEnabledSites() ?: emptyList()
    }
    
    fun getCustomSites(context: android.content.Context): List<SiteConfig> {
        ensureInitialized(context)
        return siteRepository?.getCustomSites() ?: emptyList()
    }
    
    fun addCustomSite(context: android.content.Context, name: String, url: String): Boolean {
        ensureInitialized(context)
        return siteRepository?.addCustomSite(name, url) ?: false
    }
    
    fun removeCustomSite(context: android.content.Context, siteId: String) {
        ensureInitialized(context)
        siteRepository?.removeCustomSite(siteId)
    }
    
    fun updateCustomSite(context: android.content.Context, siteId: String, name: String, url: String): Boolean {
        ensureInitialized(context)
        return siteRepository?.updateCustomSite(siteId, name, url) ?: false
    }
    
    fun removeDefaultSite(context: android.content.Context, siteId: String) {
        ensureInitialized(context)
        siteRepository?.removeDefaultSite(siteId)
    }
    
    fun saveLastSource(context: android.content.Context, source: String) {
        ensureInitialized(context)
        siteRepository?.saveLastSource(source)
    }
    
    fun getLastSource(context: android.content.Context): String {
        ensureInitialized(context)
        return siteRepository?.getLastSource() ?: "freepik"
    }
    
    private fun ensureInitialized(context: android.content.Context) {
        if (siteRepository == null) {
            initialize(context)
        }
    }
} 