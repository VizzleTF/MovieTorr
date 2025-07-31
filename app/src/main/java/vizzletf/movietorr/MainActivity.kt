package vizzletf.movietorr

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import android.view.View
import vizzletf.movietorr.data.PreferencesRepository
import vizzletf.movietorr.domain.SiteRepository
import vizzletf.movietorr.ui.MainViewModel
import vizzletf.movietorr.ui.MainViewModelFactory
import vizzletf.movietorr.ui.WebViewManager

class MainActivity : AppCompatActivity(), SettingsBottomSheet.ThemeChangeListener {
    private lateinit var webView: WebView
    private lateinit var webViewProgress: com.google.android.material.progressindicator.LinearProgressIndicator
    
    private lateinit var viewModel: MainViewModel
    private lateinit var webViewManager: WebViewManager
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var siteRepository: SiteRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализируем зависимости
        initializeDependencies()
        
        // Применяем сохраненную тему
        applyThemeMode()

        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        webViewProgress = findViewById(R.id.webViewProgress)
        
        // Инициализируем WebView
        webViewManager.setupWebView(webView)
        
        setupButtons()
        setupObservers()
    }
    
    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is MainViewModel.MainUiState.Loading -> {
                    webViewProgress.visibility = View.VISIBLE
                }
                is MainViewModel.MainUiState.LoadingUrl -> {
                    webViewProgress.visibility = View.VISIBLE
                    if (state.url.isNotEmpty()) {
                        webViewManager.loadUrl(state.url)
                    }
                }
                is MainViewModel.MainUiState.Loaded -> {
                    webViewProgress.visibility = View.GONE
                }
            }
        }
        
        viewModel.movieData.observe(this) { movieData ->
            if (movieData.success) {
                val searchSheet = SearchBottomSheet.newInstance(movieData.title)
                searchSheet.show(supportFragmentManager, "search")
            } else {
                val searchSheet = SearchBottomSheet.newInstance("")
                searchSheet.show(supportFragmentManager, "search")
            }
        }
    }
    
    private fun initializeDependencies() {
        // Инициализируем SiteManager для обратной совместимости
        SiteManager.initialize(this)
        
        preferencesRepository = PreferencesRepository(this)
        siteRepository = SiteRepository(preferencesRepository)
        
        val factory = MainViewModelFactory(siteRepository, preferencesRepository)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
        
        webViewManager = WebViewManager(
            context = this,
            preferencesRepository = preferencesRepository,
            onPageStarted = { viewModel.onPageStarted() },
            onPageFinished = { viewModel.onPageFinished() },
            onMovieDataExtracted = { title, success -> 
                runOnUiThread { viewModel.onMovieDataExtracted(title, success) }
            }
        )
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
    
    override fun onPause() {
        super.onPause()
        webViewManager.onPause()
    }
    
    override fun onResume() {
        super.onResume()
        webViewManager.onResume()
    }
    
    override fun onDestroy() {
        webViewManager.destroy()
        super.onDestroy()
    }

    private fun applyThemeMode() {
        val themeMode = preferencesRepository.getThemeMode()

        when (themeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            AppCompatDelegate.MODE_NIGHT_YES -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        
        // Применяем тему к WebView после изменения темы приложения
        webViewManager.updateTheme()
    }



    private fun setupButtons() {
        findViewById<MaterialButton>(R.id.btnSite).setOnClickListener {
            SiteBottomSheet().show(supportFragmentManager, "site")
        }
        findViewById<MaterialButton>(R.id.btnSearch).setOnClickListener {
            webViewManager.extractMovieData()
        }
        findViewById<MaterialButton>(R.id.btnSettings).setOnClickListener {
            SettingsBottomSheet().show(supportFragmentManager, "settings")
        }
    }

    fun loadSite(url: String) {
        viewModel.loadSite(url)
    }

    fun saveLastSource(source: String) {
        viewModel.saveLastSource(source)
    }


    
    override fun onThemeChanged(themeMode: Int) {
        viewModel.updateTheme(themeMode)
        webViewManager.updateTheme()
    }
}