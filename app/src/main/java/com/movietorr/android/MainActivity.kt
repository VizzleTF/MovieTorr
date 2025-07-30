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
import com.movietorr.android.databinding.DialogSettingsBinding

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
    
    private fun showSettingsDialog() {
        val dialogBinding = DialogSettingsBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()
        
        // Устанавливаем текущую тему
        val sharedPrefs = getSharedPreferences("MovieTorrPrefs", MODE_PRIVATE)
        val currentTheme = sharedPrefs.getInt(PREF_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        
        when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> dialogBinding.themeLight.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> dialogBinding.themeDark.isChecked = true
            else -> dialogBinding.themeAuto.isChecked = true
        }
        
        // Обработчики кнопок
        dialogBinding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialogBinding.applyButton.setOnClickListener {
            val newTheme = when (dialogBinding.themeRadioGroup.checkedRadioButtonId) {
                R.id.themeLight -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.themeDark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            
            sharedPrefs.edit().putInt(PREF_THEME_MODE, newTheme).apply()
            AppCompatDelegate.setDefaultNightMode(newTheme)
            dialog.dismiss()
        }
        
        dialogBinding.manageSitesButton.setOnClickListener {
            dialog.dismiss()
            showSettingsFragment()
        }
        
        dialog.show()
    }
    
    private fun showSettingsFragment() {
        webView.visibility = View.GONE
        hideFloatingButton()
        
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
                
                // Показываем плавающую кнопку на всех страницах
                showFloatingButton()
                
                binding.swipeRefresh.isRefreshing = false
            }
        }
        
        binding.swipeRefresh.setOnRefreshListener {
            webView.reload()
        }
    }
    
    private fun setupBottomBar() {
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
            showSearchDialog()
        }
        settingsButton.setOnClickListener {
            showSettingsDialog()
        }
    }
    private fun updateSiteButton() {
        val sharedPrefs = getSharedPreferences("MovieTorrPrefs", MODE_PRIVATE)
        val lastSource = sharedPrefs.getString(PREF_LAST_SOURCE, "freepik") ?: "freepik"
        val sites = SiteSettingsManager.getEnabledSites(this)
        val site = sites.find { it.id == lastSource } ?: sites.firstOrNull()
        siteButton.text = site?.name ?: "Site"
    }
    private fun showSearchDialog() {
        TorrentSearchDialog(this, torApiService).show()
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