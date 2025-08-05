package vizzletf.movietorr

data class SearchFilters(
    val enabledTrackers: Set<String> = emptySet(),
    val sizeFilterMin: Int = 0,
    val sizeFilterMax: Int = 0,
    val sizeFilterUnit: String = "MB",
    val minSeeds: Int = 0,
    val dateFilterMode: Int = DATE_FILTER_OFF
) {
    companion object {
        const val MIN_SEEDS_OFF = 0
        const val MIN_SEEDS_1 = 1
        const val MIN_SEEDS_10 = 10
        const val MIN_SEEDS_100 = 100
        
        const val SIZE_UNIT_MB = "MB"
        const val SIZE_UNIT_GB = "GB"
        
        const val DATE_FILTER_OFF = 0
        const val DATE_FILTER_DAY = 1
        const val DATE_FILTER_WEEK = 2
        const val DATE_FILTER_MONTH = 3
        const val DATE_FILTER_YEAR = 4
        
        const val SORT_DEFAULT = 0
        const val SORT_BY_SIZE = 1
        const val SORT_BY_DATE = 2
        const val SORT_BY_SEEDS = 3
        const val SORT_BY_NAME = 4
    }
    
    fun isSizeFilterEnabled(): Boolean = sizeFilterMin > 0 || sizeFilterMax > 0
    
    fun isMinSeedsEnabled(): Boolean = minSeeds > 0
    
    fun isAdvancedSearchMode(): Boolean = true
    
    fun isDateFilterEnabled(): Boolean = dateFilterMode != DATE_FILTER_OFF
}