package vizzletf.movietorr.ui

import android.content.Context
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatDelegate
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import vizzletf.movietorr.data.PreferencesRepository
import vizzletf.movietorr.SearchFilters

class WebViewManager(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val onPageStarted: () -> Unit,
    private val onPageFinished: () -> Unit,
    private val onMovieDataExtracted: (String, Boolean) -> Unit
) {
    
    private var webView: WebView? = null
    private var currentFilters: SearchFilters? = null
    private var currentUrl: String? = null
    
    fun setupWebView(webView: WebView) {
        this.webView = webView
        setupWebViewSettings()
        setupWebViewClient()
        setupWebViewDarkTheme()
        setupJavaScriptInterface()
        
        // Восстанавливаем последний URL при инициализации
        restoreLastUrl()
    }
    
    private fun setupWebViewSettings() {
        webView?.apply {
            // Аппаратное ускорение для лучшей производительности
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            // Оптимизированные настройки WebView
            settings.apply {
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
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        }
    }
    
    private fun setupWebViewClient() {
        webView?.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                // Сохраняем URL при начале загрузки страницы
                if (!url.isNullOrEmpty() && url != "about:blank") {
                    currentUrl = url
                }
                onPageStarted()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                
                // Сохраняем URL при завершении загрузки страницы
                if (!url.isNullOrEmpty() && url != "about:blank") {
                    currentUrl = url
                }
                
                // Включаем загрузку изображений после завершения основного контента
                view?.settings?.apply {
                    blockNetworkImage = false
                    loadsImagesAutomatically = true
                }
                
                onPageFinished()
            }
        }
    }
    
    private fun setupWebViewDarkTheme() {
        val themeMode = preferencesRepository.getThemeMode()
        
        // Настраиваем алгоритмическое затемнение для Android 13+
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            val isDarkTheme = when (themeMode) {
                AppCompatDelegate.MODE_NIGHT_YES -> true
                AppCompatDelegate.MODE_NIGHT_NO -> false
                else -> AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
            }
            webView?.let { WebSettingsCompat.setAlgorithmicDarkeningAllowed(it.settings, isDarkTheme) }
        }
        
        // Настраиваем Force Dark для Android 12 и ниже
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            @Suppress("DEPRECATION")
            webView?.let { webView ->
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
        }
        
        // Настраиваем стратегию темной темы
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
            @Suppress("DEPRECATION")
            webView?.let { webView ->
                WebSettingsCompat.setForceDarkStrategy(
                    webView.settings,
                    WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING
                )
            }
        }
    }
    
    private fun setupJavaScriptInterface() {
        webView?.addJavascriptInterface(WebViewInterface(), "Android")
    }
    
    fun loadUrl(url: String) {
        currentUrl = url
        // Сохраняем URL в SharedPreferences для восстановления
        context.getSharedPreferences("WebViewState", Context.MODE_PRIVATE)
            .edit()
            .putString("last_url", url)
            .apply()
        webView?.loadUrl(url)
    }
    
    fun updateSearchFilters(filters: SearchFilters) {
        currentFilters = filters
    }
    
    fun extractMovieData() {
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
                    
                    // Оставляем пробелы как есть
                    const finalTitle = title;
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
        webView?.evaluateJavascript(script, null)
    }
    
    fun updateTheme() {
        // Сохраняем текущий URL перед обновлением темы
        val savedUrl = webView?.url ?: currentUrl
        
        // Обновляем настройки темы
        setupWebViewDarkTheme()
        
        // Восстанавливаем URL если страница пустая или была перезагружена
        if (savedUrl != null && (webView?.url.isNullOrEmpty() || webView?.url == "about:blank")) {
            webView?.loadUrl(savedUrl)
            currentUrl = savedUrl
        }
    }
    
    fun restoreLastUrl() {
        // Сначала пробуем восстановить из памяти
        currentUrl?.let { url ->
            if (!url.isEmpty() && url != "about:blank") {
                webView?.loadUrl(url)
                return
            }
        }
        
        // Если в памяти нет, пробуем восстановить из SharedPreferences
        val savedUrl = context.getSharedPreferences("WebViewState", Context.MODE_PRIVATE)
            .getString("last_url", null)
        
        if (!savedUrl.isNullOrEmpty() && savedUrl != "about:blank") {
            webView?.loadUrl(savedUrl)
            currentUrl = savedUrl
        }
    }
    
    fun onPause() {
        webView?.onPause()
    }
    
    fun onResume() {
        webView?.onResume()
    }
    
    fun destroy() {
        webView?.apply {
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
        webView = null
    }
    
    private inner class WebViewInterface {
        @android.webkit.JavascriptInterface
        fun onMovieDataExtracted(title: String, year: String, success: Boolean) {
            val titleWithYear = if (year.isNotEmpty()) "$title ($year)" else title
            onMovieDataExtracted(titleWithYear, success)
        }
    }
}