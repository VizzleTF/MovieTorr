package vizzletf.movietorr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton
import android.widget.RadioButton
import android.widget.RadioGroup

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
        
        val themeRadioGroup = view.findViewById<RadioGroup>(R.id.radioTheme)
        val sortRadioGroup = view.findViewById<RadioGroup>(R.id.radioSort)
        val legalButton = view.findViewById<MaterialButton>(R.id.btnLegal)
        
        // Устанавливаем текущую тему
        setupThemeSelection(themeRadioGroup)
        
        // Устанавливаем текущую сортировку
        setupSortSelection(sortRadioGroup)
        
        // Обработка изменения темы
        themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioAuto -> setThemeMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                R.id.radioLight -> setThemeMode(AppCompatDelegate.MODE_NIGHT_NO)
                R.id.radioDark -> setThemeMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
        
        // Обработка изменения сортировки
        sortRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioSortDefault -> setSortMode(SORT_DEFAULT)
                R.id.radioSortSize -> setSortMode(SORT_SIZE)
                R.id.radioSortDate -> setSortMode(SORT_DATE)
                R.id.radioSortSeeds -> setSortMode(SORT_SEEDS)
                R.id.radioSortTracker -> setSortMode(SORT_TRACKER)
                R.id.radioSortCategory -> setSortMode(SORT_CATEGORY)
            }
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
    
    private fun setupThemeSelection(radioGroup: RadioGroup) {
        val sharedPrefs = requireContext().getSharedPreferences("MovieTorrPrefs", 0)
        val themeMode = sharedPrefs.getInt(PREF_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        
        val radioButtonId = when (themeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> R.id.radioLight
            AppCompatDelegate.MODE_NIGHT_YES -> R.id.radioDark
            else -> R.id.radioAuto
        }
        
        radioGroup.check(radioButtonId)
    }
    
    private fun setThemeMode(themeMode: Int) {
        val sharedPrefs = requireContext().getSharedPreferences("MovieTorrPrefs", 0)
        sharedPrefs.edit().putInt(PREF_THEME_MODE, themeMode).apply()
        
        AppCompatDelegate.setDefaultNightMode(themeMode)
        
        // Уведомляем MainActivity об изменении темы
        (activity as? ThemeChangeListener)?.onThemeChanged(themeMode)
    }
    
    private fun setupSortSelection(radioGroup: RadioGroup) {
        val sharedPrefs = requireContext().getSharedPreferences("MovieTorrPrefs", 0)
        val sortMode = sharedPrefs.getInt(PREF_SORT_MODE, SORT_DEFAULT)
        
        val radioButtonId = when (sortMode) {
            SORT_SIZE -> R.id.radioSortSize
            SORT_DATE -> R.id.radioSortDate
            SORT_SEEDS -> R.id.radioSortSeeds
            SORT_TRACKER -> R.id.radioSortTracker
            SORT_CATEGORY -> R.id.radioSortCategory
            else -> R.id.radioSortDefault
        }
        
        radioGroup.check(radioButtonId)
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