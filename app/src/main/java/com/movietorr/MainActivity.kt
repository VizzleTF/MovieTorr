package com.movietorr

import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.button.MaterialButton
import android.view.View

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var webViewProgress: com.google.android.material.progressindicator.LinearProgressIndicator

    companion object {
        private const val PREF_THEME_MODE = "theme_mode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Применяем сохраненную тему
        applyThemeMode()

        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        webViewProgress = findViewById(R.id.webViewProgress)
        setupWebView()
        setupButtons()

        // Загружаем последний выбранный источник
        loadLastSource()
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
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
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                android.util.Log.d("MainActivity", "onPageStarted: $url")
                webViewProgress.visibility = View.VISIBLE
                android.util.Log.d("MainActivity", "Progress visibility set to VISIBLE")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                android.util.Log.d("MainActivity", "onPageFinished: $url")
                webViewProgress.visibility = View.GONE
                android.util.Log.d("MainActivity", "Progress visibility set to GONE")
            }
        }
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadsImagesAutomatically = true
        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        webView.clearCache(true)
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        
        // Добавляем JavaScript интерфейс
        webView.addJavascriptInterface(KinopoiskInterface(), "Android")
    }

    private fun setupButtons() {
        findViewById<MaterialButton>(R.id.btnSite).setOnClickListener {
            SiteBottomSheet().show(supportFragmentManager, "site")
        }
        findViewById<MaterialButton>(R.id.btnSearch).setOnClickListener {
            // Показываем анимацию загрузки при запуске поиска
            webViewProgress.visibility = View.VISIBLE
            extractMovieDataFromPage()
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
        android.util.Log.d("MainActivity", "loadSite called with URL: $url")
        webView.loadUrl(url)
    }

    fun saveLastSource(source: String) {
        SiteManager.saveLastSource(this, source)
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

    // JavaScript Interface для взаимодействия с WebView
    inner class KinopoiskInterface {
        @android.webkit.JavascriptInterface
        fun onMovieDataExtracted(title: String, year: String, success: Boolean) {
            runOnUiThread {
                if (success) {
                    // Если удалось извлечь данные, открываем диалог с ними
                    val searchSheet = SearchBottomSheet.newInstance(title)
                    searchSheet.show(supportFragmentManager, "search")
                } else {
                    // Если не удалось извлечь данные, открываем пустой диалог
                    val searchSheet = SearchBottomSheet.newInstance("")
                    searchSheet.show(supportFragmentManager, "search")
                }
            }
        }
    }
}