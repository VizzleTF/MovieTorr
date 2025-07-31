package vizzletf.movietorr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import vizzletf.movietorr.data.PreferencesRepository
import vizzletf.movietorr.domain.SiteRepository

class MainViewModelFactory(
    private val siteRepository: SiteRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(siteRepository, preferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 