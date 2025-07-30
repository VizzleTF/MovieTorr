package com.movietorr.android

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Html
import android.view.View
import android.webkit.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.movietorr.android.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: WebView
    private lateinit var torApiService: TorApiService
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var torrentSearchFab: com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
    
    companion object {
        private const val PREF_LAST_SOURCE = "last_source"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        torApiService = TorApiService()
        setupToolbar()
        setupNavigationDrawer()
        setupWebView()
        setupFloatingButton()
        setupBackPressHandler()
        
        // Показываем правовое уведомление при первом запуске
        showLegalNoticeOnFirstLaunch()
        
        // Загружаем последний выбранный источник
        loadLastSource()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }
    }
    
    private fun setupNavigationDrawer() {
        drawerLayout = binding.drawerLayout
        navigationView = binding.navigationView
        
        updateNavigationMenu()
        
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_history -> {
                    showHistoryFragment()
                    true
                }
                R.id.nav_settings -> {
                    showSettingsFragment()
                    true
                }
                R.id.nav_legal -> {
                    showLegalInformation()
                    true
                }
                else -> {
                    // Динамические сайты
                    val sites = SiteSettingsManager.getEnabledSites(this)
                    val site = sites.find { generateMenuId(it.id) == menuItem.itemId }
                    if (site != null) {
                        loadSite(site.url, site.name)
                        saveLastSource(site.id)
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }
    
    fun updateNavigationMenu() {
        val menu = navigationView.menu
        
        // Очищаем старые сайты
        val sitesToRemove = mutableListOf<Int>()
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            if (item.itemId != R.id.nav_history && 
                item.itemId != R.id.nav_settings && 
                item.itemId != R.id.nav_legal) {
                sitesToRemove.add(item.itemId)
            }
        }
        sitesToRemove.forEach { menu.removeItem(it) }
        
        // Добавляем новые сайты в начало меню
        val sites = SiteSettingsManager.getEnabledSites(this)
        sites.forEachIndexed { index, site ->
            val menuItem = menu.add(0, generateMenuId(site.id), index, site.name)
            menuItem.setIcon(R.drawable.ic_movie)
        }
    }
    
    private fun generateMenuId(siteId: String): Int {
        return ("nav_$siteId").hashCode()
    }
    
    private fun loadSite(url: String, title: String) {
        supportActionBar?.title = title
        webView.visibility = View.VISIBLE
        binding.fragmentContainer.visibility = View.GONE
        webView.loadUrl(url)
        drawerLayout.closeDrawer(GravityCompat.START)
    }
    
    private fun showHistoryFragment() {
        supportActionBar?.title = "История поиска"
        webView.visibility = View.GONE
        hideFloatingButton()
        
        val historyFragment = SearchHistoryFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, historyFragment)
            .addToBackStack("history")
            .commit()
        
        binding.fragmentContainer.visibility = View.VISIBLE
        drawerLayout.closeDrawer(GravityCompat.START)
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
        supportActionBar?.title = "Настройки"
        webView.visibility = View.GONE
        hideFloatingButton()
        
        val settingsFragment = SettingsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, settingsFragment)
            .addToBackStack("settings")
            .commit()
        
        binding.fragmentContainer.visibility = View.VISIBLE
        drawerLayout.closeDrawer(GravityCompat.START)
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
        drawerLayout.closeDrawer(GravityCompat.START)
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
                
                // Показываем плавающую кнопку на всех страницах
                showFloatingButton()
                
                binding.swipeRefresh.isRefreshing = false
            }
        }
        
        binding.swipeRefresh.setOnRefreshListener {
            webView.reload()
        }
    }
    
    private fun setupFloatingButton() {
        torrentSearchFab = binding.torrentSearchFab
        torrentSearchFab.setOnClickListener {
            // Пытаемся извлечь данные с текущей страницы
            extractMovieDataFromPage()
        }
    }
    
    private fun showFloatingButton() {
        torrentSearchFab.visibility = View.VISIBLE
    }
    
    private fun hideFloatingButton() {
        torrentSearchFab.visibility = View.GONE
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
                        // "Название (год) — The Movie Database (TMDB)" или подобные суффиксы
                        /^(.+?)\s*\((\d{4})\)\s*[—–-]\s*[^-]+$/,
                        // "Название (год) - остальное" или "Название (год) | остальное"
                        /^(.+?)\s*\((\d{4})\)\s*[-|]/,
                        // "Название (год)" - только название и год
                        /^(.+?)\s*\((\d{4})\)\s*$/,
                        // "Название — остальное" - без года
                        /^(.+?)\s*[—–-]\s*[^-]+$/,
                        // "Название | остальное" - без года
                        /^(.+?)\s*[-|]/,
                        // Просто название без скобок и разделителей
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
                            // Kinopoisk селекторы
                            'h1[data-testid="hero-title-block__title"]',
                            'h1.titleHeader__title',
                            'h1[class*="title"]',
                            'h1',
                            '.film-header__title',
                            '.movie-header__title',
                            // IMDb селекторы
                            'h1[data-testid="hero-title-block__title"]',
                            'h1.titleHeader__title',
                            'h1[class*="title"]',
                            'h1',
                            // TMDB селекторы
                            'h2.title',
                            'h1.title',
                            '.title h1',
                            '.title h2',
                            'h1',
                            'h2',
                            // AniLiberty селекторы
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
                    const finalTitle = title.replace(/\\s+/g, '+');
                    
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
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
    
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                drawerLayout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            // Обновляем меню при возврате из настроек
            updateNavigationMenu()
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
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