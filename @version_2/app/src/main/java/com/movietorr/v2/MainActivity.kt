package com.movietorr.v2

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    
    companion object {
        private const val PREF_THEME_MODE = "theme_mode"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Применяем сохраненную тему
        applyThemeMode()
        
        setContentView(R.layout.activity_main)
        
        webView = findViewById(R.id.webView)
        setupWebView()
        setupButtons()
        
        // Загружаем последний выбранный источник
        loadLastSource()
    }
    
    private fun applyThemeMode() {
        val sharedPrefs = getSharedPreferences("MovieTorrPrefs", MODE_PRIVATE)
        val themeMode = sharedPrefs.getInt(PREF_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        
        when (themeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            AppCompatDelegate.MODE_NIGHT_YES -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
    
    private fun setupWebView() {
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadsImagesAutomatically = true
    }
    
    private fun setupButtons() {
        findViewById<MaterialButton>(R.id.btnSite).setOnClickListener {
            SiteBottomSheet().show(supportFragmentManager, "site")
        }
        findViewById<MaterialButton>(R.id.btnSearch).setOnClickListener {
            SearchBottomSheet().show(supportFragmentManager, "search")
        }
        findViewById<MaterialButton>(R.id.btnSettings).setOnClickListener {
            SettingsBottomSheet().show(supportFragmentManager, "settings")
        }
    }
    
    private fun loadLastSource() {
        val lastSource = SiteManager.getLastSource(this)
        val sites = SiteManager.getEnabledSites(this)
        val site = sites.find { it.id == lastSource }
        
        if (site != null) {
            loadSite(site.url)
        } else {
            // Если последний источник недоступен, загружаем первый доступный
            val firstSite = sites.firstOrNull()
            if (firstSite != null) {
                loadSite(firstSite.url)
            }
        }
    }
    
    fun loadSite(url: String) {
        webView.loadUrl(url)
    }
    
    fun saveLastSource(source: String) {
        SiteManager.saveLastSource(this, source)
    }
}