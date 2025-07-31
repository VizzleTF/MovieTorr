package vizzletf.movietorr

import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.google.android.material.button.MaterialButton
import android.view.View

class MainActivity : AppCompatActivity(), SettingsBottomSheet.ThemeChangeListener {
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
        // Приостанавливаем WebView при уходе из приложения
        if (::webView.isInitialized) {
            webView.onPause()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Возобновляем WebView при возвращении в приложение
        if (::webView.isInitialized) {
            webView.onResume()
        }
    }
    
    override fun onDestroy() {
        // Правильная очистка WebView для предотвращения утечек памяти
        if (::webView.isInitialized) {
            webView.apply {
                clearHistory()
                clearCache(true)
                loadUrl("about:blank")
                onPause()
                removeAllViews()
                @Suppress("DEPRECATION")
                destroyDrawingCache()
                
                // Удаляем из родительского контейнера
                (parent as? android.view.ViewGroup)?.removeView(this)
                destroy()
            }
        }
        super.onDestroy()
    }

    private fun applyThemeMode() {
        val sharedPrefs = getSharedPreferences("MovieTorrPrefs", MODE_PRIVATE)
        val themeMode = sharedPrefs.getInt(PREF_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        when (themeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            AppCompatDelegate.MODE_NIGHT_YES -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        
        // Применяем тему к WebView после изменения темы приложения
        if (::webView.isInitialized) {
            setupWebViewDarkTheme()
        }
    }
    
    private fun applyWebViewTheme(@Suppress("UNUSED_PARAMETER") themeMode: Int) {
        // Применяем настройки темной темы к WebView
        if (::webView.isInitialized) {
            setupWebViewDarkTheme()
        }
    }

    private fun setupWebView() {
        // Аппаратное ускорение для лучшей производительности
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        
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
                
                // Включаем загрузку изображений после завершения основного контента
                view?.settings?.apply {
                    blockNetworkImage = false
                    loadsImagesAutomatically = true
                }
                
                // Применяем тему к загруженной странице
                val sharedPrefs = getSharedPreferences("MovieTorrPrefs", MODE_PRIVATE)
                val themeMode = sharedPrefs.getInt(PREF_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                applyWebViewTheme(themeMode)
            }
        }
        
        // Оптимизированные настройки WebView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            
            // Кеширование для лучшей производительности
            cacheMode = WebSettings.LOAD_DEFAULT
            
            // Отложенная загрузка изображений для ускорения
            blockNetworkImage = true
            loadsImagesAutomatically = false
            
            // Оптимизация рендеринга
            useWideViewPort = true
            loadWithOverviewMode = true
            
            // Отключаем ненужные функции
            javaScriptCanOpenWindowsAutomatically = false
        }
        
        // Настройки кеширования
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        
        // Настраиваем темную тему для WebView
        setupWebViewDarkTheme()
        
        // Добавляем JavaScript интерфейс
        webView.addJavascriptInterface(KinopoiskInterface(), "Android")
    }
    
    private fun setupWebViewDarkTheme() {
        val sharedPrefs = getSharedPreferences("MovieTorrPrefs", MODE_PRIVATE)
        val themeMode = sharedPrefs.getInt(PREF_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        
        // Настраиваем алгоритмическое затемнение для Android 13+
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            val isDarkTheme = when (themeMode) {
                AppCompatDelegate.MODE_NIGHT_YES -> true
                AppCompatDelegate.MODE_NIGHT_NO -> false
                else -> AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
            }
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, isDarkTheme)
        }
        
        // Настраиваем Force Dark для Android 12 и ниже
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            @Suppress("DEPRECATION")
            when (themeMode) {
                AppCompatDelegate.MODE_NIGHT_YES -> {
                    WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_ON)
                }
                AppCompatDelegate.MODE_NIGHT_NO -> {
                    WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_OFF)
                }
                else -> {
                    WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_AUTO)
                }
            }
        }
        
        // Настраиваем стратегию темной темы
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
            @Suppress("DEPRECATION")
            WebSettingsCompat.setForceDarkStrategy(
                webView.settings,
                WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING
            )
        }
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
                
                function extractTitleFromJsonLd() {
                    const currentUrl = window.location.href;
                    if (!currentUrl.includes('kinopoisk.ru') && !currentUrl.includes('hd.kinopoisk.ru')) {
                        return null;
                    }
                    
                    const jsonLdScripts = document.querySelectorAll('script[type="application/ld+json"]');
                    for (const script of jsonLdScripts) {
                        try {
                            const data = JSON.parse(script.textContent);
                            if (data && data.name) {
                                return data.name.trim();
                            }
                        } catch (e) {
                            // Игнорируем ошибки парсинга JSON
                        }
                    }
                    return null;
                }
                
                // Функция для извлечения названия
                function extractMovieData() {
                    let title = '';
                    let year = '';
                    
                    // Сначала пробуем извлечь из JSON-LD данных (только для Кинопоиск)
                    title = extractTitleFromJsonLd();
                    
                    // Если не удалось извлечь из JSON-LD, пробуем из title страницы
                    if (!title) {
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
                    
                    // Очищаем название от лишних символов и слов
                    if (title) {
                        // Обрезаем до первой открывающей скобки
                        const openBracketIndex = title.indexOf('(');
                        if (openBracketIndex > 0) {
                            title = title.substring(0, openBracketIndex).trim();
                        }
                        
                        // Убираем "смотреть", "онлайн", "в хорошем качестве" и подобные слова
                        title = title.replace(/\s*(смотреть|онлайн|в\s+хорошем\s+качестве|все\s+серии?|сериал|фильм|hd|fullhd|1080p|720p|480p)\s*/gi, ' ');
                        // Убираем лишние символы (но не скобки, так как мы уже обрезали)
                        title = title.replace(/[+\[\]{}|\\\/]/g, ' ');
                        // Убираем множественные пробелы
                        title = title.replace(/\s+/g, ' ').trim();
                        // Ограничиваем длину названия (максимум 50 символов)
                        if (title.length > 50) {
                            title = title.substring(0, 50).trim();
                            // Убираем последнее неполное слово
                            const lastSpace = title.lastIndexOf(' ');
                            if (lastSpace > 0) {
                                title = title.substring(0, lastSpace);
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
                        fun onMovieDataExtracted(title: String, @Suppress("UNUSED_PARAMETER") unusedYear: String, success: Boolean) {
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
    
    override fun onThemeChanged(themeMode: Int) {
        // Применяем тему к WebView при изменении в настройках
        if (::webView.isInitialized) {
            setupWebViewDarkTheme()
        }
    }
}