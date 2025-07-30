package com.movietorr.android

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.text.Html
import android.view.View
import android.webkit.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.movietorr.android.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: WebView
    private lateinit var torApiService: TorApiService
    private lateinit var siteButton: android.widget.TextView
    private lateinit var searchButton: android.widget.ImageButton
    private lateinit var settingsButton: android.widget.ImageButton
    
    companion object {
        private const val PREF_LAST_SOURCE = "last_source"
        private const val PREF_THEME_MODE = "theme_mode"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Применяем сохраненную тему
        applyThemeMode()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        torApiService = TorApiService()
        siteButton = findViewById(R.id.siteButton)
        searchButton = findViewById(R.id.searchButton)
        settingsButton = findViewById(R.id.settingsButton)
        setupWebView()
        setupBottomBar()
        setupBackPressHandler()
        
        // Показываем правовое уведомление при первом запуске
        showLegalNoticeOnFirstLaunch()
        
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
    
    private fun loadSite(url: String, title: String) {
        webView.visibility = View.VISIBLE
        binding.fragmentContainer.visibility = View.GONE
        webView.loadUrl(url)
    }
    
    private fun saveLastSource(source: String) {
        val sharedPrefs = getSharedPreferences("MovieTorrPrefs", MODE_PRIVATE)
        sharedPrefs.edit().putString(PREF_LAST_SOURCE, source).apply()
    }
    
    private fun loadLastSource() {
        val sharedPrefs = getSharedPreferences("MovieTorrPrefs", MODE_PRIVATE)
        val lastSource = sharedPrefs.getString(PREF_LAST_SOURCE, "freepik") ?: "freepik"
        
        val sites = SiteSettingsManager.getEnabledSites(this)
        val site = sites.find { it.id == lastSource }
        
        if (site != null) {
            loadSite(site.url, site.name)
        } else {
            // Если последний источник недоступен, загружаем первый доступный
            val firstSite = sites.firstOrNull()
            if (firstSite != null) {
                loadSite(firstSite.url, firstSite.name)
            }
        }
    }
    
    private fun showSettingsFragment() {
        webView.visibility = View.GONE
        
        val settingsFragment = SettingsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, settingsFragment)
            .addToBackStack("settings")
            .commit()
        
        binding.fragmentContainer.visibility = View.VISIBLE
    }
    
    private fun showLegalInformation() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.legal_notice_title)
            .setMessage(Html.fromHtml(getString(R.string.legal_notice_content), Html.FROM_HTML_MODE_COMPACT))
            .setPositiveButton(R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .create()
        
        dialog.show()
    }
    
    private fun showLegalNoticeOnFirstLaunch() {
        val sharedPrefs = getSharedPreferences("MovieTorrPrefs", MODE_PRIVATE)
        val hasShownLegalNotice = sharedPrefs.getBoolean("has_shown_legal_notice", false)
        
        if (!hasShownLegalNotice) {
            val dialog = MaterialAlertDialogBuilder(this)
                .setTitle(R.string.legal_dialog_title)
                .setMessage(R.string.legal_dialog_message)
                .setPositiveButton(R.string.legal_dialog_accept) { dialog, _ ->
                    // Отмечаем, что уведомление показано
                    sharedPrefs.edit().putBoolean("has_shown_legal_notice", true).apply()
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.legal_dialog_decline) { dialog, _ ->
                    // Закрываем приложение, если пользователь не принимает условия
                    finish()
                }
                .setCancelable(false)
                .create()
            
            dialog.show()
        }
    }
    
    fun openTorrentSearch(query: String, year: String) {
        // Переключаемся на первый доступный сайт если находимся в истории
        if (webView.visibility != View.VISIBLE) {
            val sites = SiteSettingsManager.getEnabledSites(this)
            val firstSite = sites.firstOrNull()
            if (firstSite != null) {
                loadSite(firstSite.url, firstSite.name)
            }
        }
        
        // Добавляем в историю
        SearchHistoryManager.addToHistory(this, query)
        
        // Открываем диалог поиска с автоматическим запуском
        TorrentSearchDialog(this, torApiService, query, year, autoSearch = true).show()
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView = binding.webView
        
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
        }
        
        webView.addJavascriptInterface(KinopoiskInterface(), "Android")
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                
                binding.swipeRefresh.isRefreshing = false
            }
        }
        
        binding.swipeRefresh.setOnRefreshListener {
            webView.reload()
        }
    }
    
    private fun setupBottomBar() {
        // Центрируем нижнее меню программно
        val bottomCardView = findViewById<com.google.android.material.card.MaterialCardView>(R.id.bottomCardView)
        bottomCardView?.let { cardView ->
            val layoutParams = cardView.layoutParams as? androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
            layoutParams?.gravity = android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.BOTTOM
            cardView.layoutParams = layoutParams
        }
        
        updateSiteButton()
        siteButton.setOnClickListener {
            val sites = SiteSettingsManager.getEnabledSites(this)
            val popup = android.widget.PopupMenu(this, siteButton)
            sites.forEachIndexed { i, site ->
                popup.menu.add(0, i, i, site.name)
            }
            popup.setOnMenuItemClickListener { item ->
                val sitesList = SiteSettingsManager.getEnabledSites(this)
                val site = sitesList.getOrNull(item.itemId)
                if (site != null) {
                    saveLastSource(site.id)
                    updateSiteButton()
                    loadSite(site.url, site.name)
                }
                true
            }
            popup.show()
        }
        searchButton.setOnClickListener {
            extractMovieDataFromPage()
        }
        settingsButton.setOnClickListener {
            val dialog = SettingsFragment()
            dialog.show(supportFragmentManager, "SettingsDialog")
        }
    }
    
    private fun updateSiteButton() {
        val sharedPrefs = getSharedPreferences("MovieTorrPrefs", MODE_PRIVATE)
        val lastSource = sharedPrefs.getString(PREF_LAST_SOURCE, "freepik") ?: "freepik"
        val sites = SiteSettingsManager.getEnabledSites(this)
        val site = sites.find { it.id == lastSource } ?: sites.firstOrNull()
        siteButton.text = site?.name ?: "Site"
    }
    
    private fun extractMovieDataFromPage() {
        val script = """
            (function() {
                'use strict';
                // Функция для извлечения названия
                function extractMovieData() {
                    let title = '';
                    let year = '';
                    // Сначала пробуем извлечь из title страницы
                    const pageTitle = document.title.trim();
                    // Паттерны для извлечения названия и года из title
                    const titlePatterns = [
                        /^(.+?)\s*\((\d{4})\)\s*[—–-]\s*[^-]+$/,
                        /^(.+?)\s*\((\d{4})\)\s*[-|]/,
                        /^(.+?)\s*\((\d{4})\)\s*$/,
                        /^(.+?)\s*[—–-]\s*[^-]+$/,
                        /^(.+?)\s*[-|]/,
                        /^(.+?)\s*$/
                    ];
                    for (const pattern of titlePatterns) {
                        const match = pageTitle.match(pattern);
                        if (match) {
                            title = match[1].trim();
                            if (match[2]) {
                                year = match[2];
                            }
                            break;
                        }
                    }
                    // Если не удалось извлечь из title, пробуем DOM элементы
                    if (!title) {
                        const titleSelectors = [
                            'h1[data-testid="hero-title-block__title"]',
                            'h1.titleHeader__title',
                            'h1[class*="title"]',
                            'h1',
                            '.film-header__title',
                            '.movie-header__title',
                            'h1[data-testid="hero-title-block__title"]',
                            'h1.titleHeader__title',
                            'h1[class*="title"]',
                            'h1',
                            'h2.title',
                            'h1.title',
                            '.title h1',
                            '.title h2',
                            'h1',
                            'h2',
                            '.release-name',
                            '.anime-title',
                            'h1.release-name',
                            'h1.anime-title',
                            '.title h1',
                            '.title h2',
                            'h1',
                            'h2'
                        ];
                        for (const selector of titleSelectors) {
                            const element = document.querySelector(selector);
                            if (element) {
                                title = element.textContent.trim();
                                break;
                            }
                        }
                    }
                    // Заменяем пробелы на + для URL
                    const finalTitle = title.replace(/\s+/g, '+');
                    return { 
                        title: finalTitle, 
                        year: year, 
                        success: finalTitle.length > 0
                    };
                }
                const result = extractMovieData();
                Android.onMovieDataExtracted(result.title, result.year, result.success);
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }
    
    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
    
    // JavaScript Interface для взаимодействия с WebView
    inner class KinopoiskInterface {
        @JavascriptInterface
        fun onMovieDataExtracted(title: String, year: String, success: Boolean) {
            runOnUiThread {
                if (success) {
                    // Если удалось извлечь данные, открываем диалог с ними
                    openTorrentSearch(title, year)
                } else {
                    // Если не удалось извлечь данные, открываем пустой диалог
                    openTorrentSearch("", "")
                }
            }
        }
    }
} 