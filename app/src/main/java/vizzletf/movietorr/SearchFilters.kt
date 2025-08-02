package vizzletf.movietorr

data class SearchFilters(
    val enabledTrackers: Set<String> = emptySet(),
    val sizeFilterMin: Int = 0,
    val sizeFilterMax: Int = 0,
    val sizeFilterUnit: String = "MB",
    val searchMode: Int = 0,
    val minSeeds: Int = 0
) {
    companion object {
        const val SEARCH_MODE_STANDARD = 0
        const val SEARCH_MODE_ADVANCED = 1
        
        const val MIN_SEEDS_OFF = 0
        const val MIN_SEEDS_1 = 1
        const val MIN_SEEDS_10 = 10
        const val MIN_SEEDS_100 = 100
        
        const val SIZE_UNIT_MB = "MB"
        const val SIZE_UNIT_GB = "GB"
    }
    
    fun isSizeFilterEnabled(): Boolean = sizeFilterMin > 0 || sizeFilterMax > 0
    
    fun isMinSeedsEnabled(): Boolean = minSeeds > 0
    
    fun isAdvancedSearchMode(): Boolean = searchMode == SEARCH_MODE_ADVANCED
} 