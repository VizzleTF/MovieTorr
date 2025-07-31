package vizzletf.movietorr.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import vizzletf.movietorr.SiteConfig
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
    
    init {
        loadLastSource()
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
    
    sealed class MainUiState {
        object Loading : MainUiState()
        object Loaded : MainUiState()
        data class LoadingUrl(val url: String) : MainUiState()
    }
    
    data class MovieData(
        val title: String,
        val success: Boolean
    )
} 