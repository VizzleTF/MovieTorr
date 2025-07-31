package vizzletf.movietorr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.ArrayAdapter
import android.widget.Spinner

class SettingsBottomSheet : BottomSheetDialogFragment() {
    
    companion object {
        private const val PREF_THEME_MODE = "theme_mode"
        private const val PREF_SORT_MODE = "sort_mode"
        
        // Константы для сортировки
        const val SORT_DEFAULT = 0
        const val SORT_SIZE = 1
        const val SORT_DATE = 2
        const val SORT_SEEDS = 3
        const val SORT_TRACKER = 4
        const val SORT_CATEGORY = 5
    }
    
    interface ThemeChangeListener {
        fun onThemeChanged(themeMode: Int)
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_settings, container, false)
        
        val themeSpinner = view.findViewById<Spinner>(R.id.spinnerTheme)
        val sortSpinner = view.findViewById<Spinner>(R.id.spinnerSort)
        val legalButton = view.findViewById<MaterialButton>(R.id.btnLegal)
        
        // Устанавливаем текущую тему
        setupThemeSelection(themeSpinner)
        
        // Устанавливаем текущую сортировку
        setupSortSelection(sortSpinner)
        
        // Обработка изменения темы
        themeSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> setThemeMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    1 -> setThemeMode(AppCompatDelegate.MODE_NIGHT_NO)
                    2 -> setThemeMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        
        // Обработка изменения сортировки
        sortSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> setSortMode(SORT_DEFAULT)
                    1 -> setSortMode(SORT_SIZE)
                    2 -> setSortMode(SORT_DATE)
                    3 -> setSortMode(SORT_SEEDS)
                    4 -> setSortMode(SORT_TRACKER)
                    5 -> setSortMode(SORT_CATEGORY)
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        
        // Обработка кнопки правовой информации
        legalButton.setOnClickListener {
            showLegalInfo()
        }
        
        return view
    }
    
    override fun onStart() {
        super.onStart()
        val dialog = dialog as? com.google.android.material.bottomsheet.BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(it)
            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        }
    }
    
    private fun setupThemeSelection(spinner: Spinner) {
        val themeOptions = arrayOf(
            getString(R.string.settings_theme_auto),
            getString(R.string.settings_theme_light),
            getString(R.string.settings_theme_dark)
        )
        
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, themeOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        
        val sharedPrefs = requireContext().getSharedPreferences("MovieTorrPrefs", 0)
        val themeMode = sharedPrefs.getInt(PREF_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        
        val position = when (themeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> 1
            AppCompatDelegate.MODE_NIGHT_YES -> 2
            else -> 0
        }
        
        spinner.setSelection(position)
    }
    
    private fun setThemeMode(themeMode: Int) {
        val sharedPrefs = requireContext().getSharedPreferences("MovieTorrPrefs", 0)
        sharedPrefs.edit().putInt(PREF_THEME_MODE, themeMode).apply()
        
        AppCompatDelegate.setDefaultNightMode(themeMode)
        
        // Уведомляем MainActivity об изменении темы
        (activity as? ThemeChangeListener)?.onThemeChanged(themeMode)
    }
    
    private fun setupSortSelection(spinner: Spinner) {
        val sortOptions = arrayOf(
            getString(R.string.settings_sort_default),
            getString(R.string.settings_sort_size),
            getString(R.string.settings_sort_date),
            getString(R.string.settings_sort_seeds),
            getString(R.string.settings_sort_tracker),
            getString(R.string.settings_sort_category)
        )
        
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        
        val sharedPrefs = requireContext().getSharedPreferences("MovieTorrPrefs", 0)
        val sortMode = sharedPrefs.getInt(PREF_SORT_MODE, SORT_DEFAULT)
        
        val position = when (sortMode) {
            SORT_SIZE -> 1
            SORT_DATE -> 2
            SORT_SEEDS -> 3
            SORT_TRACKER -> 4
            SORT_CATEGORY -> 5
            else -> 0
        }
        
        spinner.setSelection(position)
    }
    
    private fun setSortMode(sortMode: Int) {
        val sharedPrefs = requireContext().getSharedPreferences("MovieTorrPrefs", 0)
        sharedPrefs.edit().putInt(PREF_SORT_MODE, sortMode).apply()
    }
    
    private fun showLegalInfo() {
                MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.settings_legal_info))
            .setMessage(getString(R.string.settings_legal_message))
            .setPositiveButton(getString(R.string.settings_legal_ok), null)
            .show()
    }
}