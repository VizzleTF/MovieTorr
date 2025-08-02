package vizzletf.movietorr.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import vizzletf.movietorr.SiteConfig
import vizzletf.movietorr.SearchFilters
import vizzletf.movietorr.data.PreferencesRepository
import vizzletf.movietorr.domain.SiteRepository

class MainViewModel(
    private val siteRepository: SiteRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    private val _uiState = MutableLiveData<MainUiState>()
    val uiState: LiveData<MainUiState> = _uiState
    
    private val _movieData = MutableLiveData<MovieData>()
    val movieData: LiveData<MovieData> = _movieData
    
    private val _searchFilters = MutableLiveData<SearchFilters>()
    val searchFilters: LiveData<SearchFilters> = _searchFilters
    
    init {
        loadLastSource()
        loadSearchFilters()
    }
    
    fun loadLastSource() {
        viewModelScope.launch {
            val lastSource = siteRepository.getLastSource()
            val sites = siteRepository.getEnabledSites()
            val site = sites.find { it.id == lastSource }
            
            val url = if (site != null) {
                site.url
            } else {
                sites.firstOrNull()?.url ?: ""
            }
            
            _uiState.value = MainUiState.LoadingUrl(url)
        }
    }
    
    fun loadSite(url: String) {
        _uiState.value = MainUiState.LoadingUrl(url)
    }
    
    fun onPageStarted() {
        _uiState.value = MainUiState.Loading
    }
    
    fun onPageFinished() {
        _uiState.value = MainUiState.Loaded
    }
    
    fun onMovieDataExtracted(title: String, success: Boolean) {
        _movieData.value = MovieData(title, success)
    }
    
    fun saveLastSource(source: String) {
        siteRepository.saveLastSource(source)
    }
    
    fun updateTheme(themeMode: Int) {
        preferencesRepository.setThemeMode(themeMode)
    }
    
    fun loadSearchFilters() {
        val filters = SearchFilters(
            enabledTrackers = preferencesRepository.getEnabledTrackers(),
            sizeFilterMin = preferencesRepository.getSizeFilterMin(),
            sizeFilterMax = preferencesRepository.getSizeFilterMax(),
            sizeFilterUnit = preferencesRepository.getSizeFilterUnit(),
            searchMode = preferencesRepository.getSearchMode(),
            minSeeds = preferencesRepository.getMinSeeds()
        )
        _searchFilters.value = filters
    }
    
    fun applyFiltersToResults(results: List<TorrentResult>): List<TorrentResult> {
        val filters = _searchFilters.value ?: return results
        
        return results.filter { torrent ->
            // Фильтр по трекерам
            if (filters.enabledTrackers.isNotEmpty() && !filters.enabledTrackers.contains(torrent.tracker)) {
                return@filter false
            }
            
            // Фильтр по размеру
            if (filters.isSizeFilterEnabled()) {
                val torrentSizeInMB = convertSizeToMB(torrent.size, torrent.sizeUnit)
                val minSizeInMB = if (filters.sizeFilterMin > 0) {
                    convertSizeToMB(filters.sizeFilterMin, filters.sizeFilterUnit)
                } else 0
                val maxSizeInMB = if (filters.sizeFilterMax > 0) {
                    convertSizeToMB(filters.sizeFilterMax, filters.sizeFilterUnit)
                } else Int.MAX_VALUE
                
                if (torrentSizeInMB < minSizeInMB || torrentSizeInMB > maxSizeInMB) {
                    return@filter false
                }
            }
            
            // Фильтр по минимальному количеству сидов
            if (filters.isMinSeedsEnabled() && torrent.seeds < filters.minSeeds) {
                return@filter false
            }
            
            true
        }
    }
    
    private fun convertSizeToMB(size: Int, unit: String): Int {
        return when (unit.uppercase()) {
            "GB" -> size * 1024
            "MB" -> size
            "KB" -> size / 1024
            else -> size
        }
    }
    
    sealed class MainUiState {
        object Loading : MainUiState()
        object Loaded : MainUiState()
        data class LoadingUrl(val url: String) : MainUiState()
    }
    
    data class MovieData(
        val title: String,
        val success: Boolean
    )
    
    data class TorrentResult(
        val title: String,
        val tracker: String,
        val size: Int,
        val sizeUnit: String,
        val seeds: Int,
        val peers: Int,
        val date: String,
        val magnetLink: String
    )
} 