package vizzletf.movietorr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.TextView
import android.widget.LinearLayout
import vizzletf.movietorr.data.PreferencesRepository
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.AutoCompleteTextView

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
    
    interface FiltersChangeListener {
        fun onFiltersChanged()
    }
    
    private lateinit var preferencesRepository: PreferencesRepository
    private val availableTrackers = listOf("RuTracker", "RuTor", "NoNameClub", "Kinozal")
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_settings, container, false)
        
        preferencesRepository = PreferencesRepository(requireContext())
        
        // Находим новые элементы интерфейса
        val themeValue = view.findViewById<TextView>(R.id.themeValue)
        val sortValue = view.findViewById<TextView>(R.id.sortValue)
        val minSeedsValue = view.findViewById<TextView>(R.id.minSeedsValue)
        val trackersValue = view.findViewById<TextView>(R.id.trackersValue)
        val sizeFilterValue = view.findViewById<TextView>(R.id.sizeFilterValue)
        val dateFilterValue = view.findViewById<TextView>(R.id.dateFilterValue)
        val legalButton = view.findViewById<MaterialButton>(R.id.btnLegal)
        
        // Устанавливаем текущие значения
        setupCurrentValues(themeValue, sortValue, minSeedsValue, trackersValue, sizeFilterValue, dateFilterValue)
        
        // Обработчики кликов для новых элементов
        setupClickHandlers(view, themeValue, sortValue, minSeedsValue, trackersValue, sizeFilterValue)
        
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
            behavior.isDraggable = true
            behavior.isHideable = true
            behavior.skipCollapsed = false
            behavior.halfExpandedRatio = 0.5f
            behavior.expandedOffset = 0
        }
    }
    
    private fun setupCurrentValues(
        themeValue: TextView,
        sortValue: TextView,
        minSeedsValue: TextView,
        trackersValue: TextView,
        sizeFilterValue: TextView,
        dateFilterValue: TextView
    ) {
        // Тема
        val themeMode = preferencesRepository.getThemeMode()
        val themeText = when (themeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> getString(R.string.settings_theme_light)
            AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.settings_theme_dark)
            else -> getString(R.string.settings_theme_auto)
        }
        themeValue.text = themeText
        
        // Сортировка
        val sharedPrefs = requireContext().getSharedPreferences("MovieTorrPrefs", 0)
        val actualSortMode = sharedPrefs.getInt(PREF_SORT_MODE, SORT_DEFAULT)
        val sortText = when (actualSortMode) {
            SORT_SIZE -> getString(R.string.settings_sort_size)
            SORT_DATE -> getString(R.string.settings_sort_date)
            SORT_SEEDS -> getString(R.string.settings_sort_seeds)
            SORT_TRACKER -> getString(R.string.settings_sort_tracker)
            SORT_CATEGORY -> getString(R.string.settings_sort_category)
            else -> getString(R.string.settings_sort_default)
        }
        sortValue.text = sortText
        
        
        // Минимальные сиды
        val minSeeds = preferencesRepository.getMinSeeds()
        val minSeedsText = when (minSeeds) {
            SearchFilters.MIN_SEEDS_1 -> getString(R.string.settings_min_seeds_1)
            SearchFilters.MIN_SEEDS_10 -> getString(R.string.settings_min_seeds_10)
            SearchFilters.MIN_SEEDS_100 -> getString(R.string.settings_min_seeds_100)
            else -> getString(R.string.settings_min_seeds_off)
        }
        minSeedsValue.text = minSeedsText
        
        // Трекеры
        val enabledTrackers = preferencesRepository.getEnabledTrackers()
        val trackersText = if (enabledTrackers.size == availableTrackers.size) {
            "Все трекеры"
        } else if (enabledTrackers.isEmpty()) {
            "Нет трекеров"
        } else {
            "${enabledTrackers.size} трекеров"
        }
        trackersValue.text = trackersText
        
        // Фильтр даты
        val dateFilterMode = preferencesRepository.getDateFilterMode()
        val dateFilterText = when (dateFilterMode) {
            SearchFilters.DATE_FILTER_DAY -> getString(R.string.filter_date_day)
            SearchFilters.DATE_FILTER_WEEK -> getString(R.string.filter_date_week)
            SearchFilters.DATE_FILTER_MONTH -> getString(R.string.filter_date_month)
            SearchFilters.DATE_FILTER_YEAR -> getString(R.string.filter_date_year)
            else -> getString(R.string.filter_date_off)
        }
        dateFilterValue.text = dateFilterText
        
        // Фильтр размера
        val minSize = preferencesRepository.getSizeFilterMin()
        val maxSize = preferencesRepository.getSizeFilterMax()
        val sizeUnit = preferencesRepository.getSizeFilterUnit()
        val sizeText = when {
            minSize == 0 && maxSize == 0 -> "Без ограничений"
            minSize > 0 && maxSize == 0 -> "От ${minSize}${if (sizeUnit == SearchFilters.SIZE_UNIT_MB) " МБ" else " ГБ"}"
            minSize == 0 && maxSize > 0 -> "До ${maxSize}${if (sizeUnit == SearchFilters.SIZE_UNIT_MB) " МБ" else " ГБ"}"
            else -> "${minSize}-${maxSize}${if (sizeUnit == SearchFilters.SIZE_UNIT_MB) " МБ" else " ГБ"}"
        }
        sizeFilterValue.text = sizeText
    }
    
    private fun setupClickHandlers(
        view: View,
        themeValue: TextView,
        sortValue: TextView,
        minSeedsValue: TextView,
        trackersValue: TextView,
        sizeFilterValue: TextView
    ) {
        // Обработчик для темы
        view.findViewById<LinearLayout>(R.id.themeContainer)?.setOnClickListener {
            showThemeDialog(themeValue)
        }
        
        // Обработчик для сортировки
        view.findViewById<LinearLayout>(R.id.sortContainer)?.setOnClickListener {
            showSortDialog(sortValue)
        }
        
        
        // Обработчик для минимальных сидов
        view.findViewById<LinearLayout>(R.id.minSeedsContainer)?.setOnClickListener {
            showMinSeedsDialog(minSeedsValue)
        }
        
        // Обработчик для трекеров
        view.findViewById<LinearLayout>(R.id.trackersContainer)?.setOnClickListener {
            showTrackersDialog(trackersValue)
        }
        
        // Обработчик для фильтра размера
        view.findViewById<LinearLayout>(R.id.sizeFilterContainer)?.setOnClickListener {
            showSizeFilterDialog(sizeFilterValue)
        }
        
        // Обработчик для фильтра даты
        view.findViewById<LinearLayout>(R.id.dateFilterContainer)?.setOnClickListener {
            showDateFilterDialog(view.findViewById(R.id.dateFilterValue))
        }
    }
    
    private fun showThemeDialog(themeValue: TextView) {
        val themes = arrayOf(
            getString(R.string.settings_theme_auto),
            getString(R.string.settings_theme_light),
            getString(R.string.settings_theme_dark)
        )
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.settings_theme))
            .setItems(themes) { _, which ->
                val themeMode = when (which) {
                    0 -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    2 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                setThemeMode(themeMode)
                themeValue.text = themes[which]
            }
            .show()
    }
    
    private fun showSortDialog(sortValue: TextView) {
        val sortOptions = arrayOf(
            getString(R.string.settings_sort_default),
            getString(R.string.settings_sort_size),
            getString(R.string.settings_sort_date),
            getString(R.string.settings_sort_seeds),
            getString(R.string.settings_sort_tracker),
            getString(R.string.settings_sort_category)
        )
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.settings_sort))
            .setItems(sortOptions) { _, which ->
                setSortMode(which)
                sortValue.text = sortOptions[which]
            }
            .show()
    }
    

    
    private fun showMinSeedsDialog(minSeedsValue: TextView) {
        val minSeedsOptions = arrayOf(
            getString(R.string.settings_min_seeds_off),
            getString(R.string.settings_min_seeds_1),
            getString(R.string.settings_min_seeds_10),
            getString(R.string.settings_min_seeds_100)
        )
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.settings_min_seeds))
            .setItems(minSeedsOptions) { _, which ->
                val minSeeds = when (which) {
                    0 -> SearchFilters.MIN_SEEDS_OFF
                    1 -> SearchFilters.MIN_SEEDS_1
                    2 -> SearchFilters.MIN_SEEDS_10
                    3 -> SearchFilters.MIN_SEEDS_100
                    else -> SearchFilters.MIN_SEEDS_OFF
                }
                preferencesRepository.setMinSeeds(minSeeds)
                minSeedsValue.text = minSeedsOptions[which]
                notifyFiltersChanged()
            }
            .show()
    }
    
    private fun showTrackersDialog(trackersValue: TextView) {
        val enabledTrackers = preferencesRepository.getEnabledTrackers()
        val checkedItems = BooleanArray(availableTrackers.size) { i ->
            enabledTrackers.contains(availableTrackers[i])
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.settings_trackers))
            .setMultiChoiceItems(availableTrackers.toTypedArray(), checkedItems) { _, which, isChecked ->
                val currentTrackers = preferencesRepository.getEnabledTrackers().toMutableSet()
                if (isChecked) {
                    currentTrackers.add(availableTrackers[which])
                } else {
                    currentTrackers.remove(availableTrackers[which])
                }
                preferencesRepository.setEnabledTrackers(currentTrackers)
                
                // Обновляем текст
                val trackersText = if (currentTrackers.size == availableTrackers.size) {
                    "Все трекеры"
                } else if (currentTrackers.isEmpty()) {
                    "Нет трекеров"
                } else {
                    "${currentTrackers.size} трекеров"
                }
                trackersValue.text = trackersText
                notifyFiltersChanged()
            }
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showSizeFilterDialog(sizeFilterValue: TextView) {
        // Создаем диалог для настройки фильтра размера
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_size_filter, null)
        
        val editSizeMin = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editSizeMin)
        val editSizeMax = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editSizeMax)
        val sizeUnitDropdown = dialogView.findViewById<android.widget.AutoCompleteTextView>(R.id.spinnerSizeUnit)
        
        // Устанавливаем текущие значения
        val minValue = preferencesRepository.getSizeFilterMin()
        val maxValue = preferencesRepository.getSizeFilterMax()
        val currentUnit = preferencesRepository.getSizeFilterUnit()
        
        if (minValue > 0) editSizeMin.setText(minValue.toString())
        if (maxValue > 0) editSizeMax.setText(maxValue.toString())
        
        // Настройка единиц измерения
        val unitOptions = arrayOf(
            getString(R.string.settings_size_filter_mb),
            getString(R.string.settings_size_filter_gb)
        )
        
        val unitAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, unitOptions)
        sizeUnitDropdown.setAdapter(unitAdapter)
        
        val unitPosition = if (currentUnit == SearchFilters.SIZE_UNIT_MB) 0 else 1
        sizeUnitDropdown.setText(unitOptions[unitPosition], false)
        
        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                // Сохраняем настройки размера
                val minSize = editSizeMin.text.toString().toIntOrNull() ?: 0
                val maxSize = editSizeMax.text.toString().toIntOrNull() ?: 0
                val unit = if (sizeUnitDropdown.text.toString() == getString(R.string.settings_size_filter_mb)) {
                    SearchFilters.SIZE_UNIT_MB
                } else {
                    SearchFilters.SIZE_UNIT_GB
                }
                
                preferencesRepository.setSizeFilterMin(minSize)
                preferencesRepository.setSizeFilterMax(maxSize)
                preferencesRepository.setSizeFilterUnit(unit)
                
                updateSizeFilterText(sizeFilterValue)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun updateSizeFilterText(sizeFilterValue: TextView) {
        val minSize = preferencesRepository.getSizeFilterMin()
        val maxSize = preferencesRepository.getSizeFilterMax()
        val sizeUnit = preferencesRepository.getSizeFilterUnit()
        val sizeText = when {
            minSize == 0 && maxSize == 0 -> "Без ограничений"
            minSize > 0 && maxSize == 0 -> "От ${minSize}${if (sizeUnit == SearchFilters.SIZE_UNIT_MB) " МБ" else " ГБ"}"
            minSize == 0 && maxSize > 0 -> "До ${maxSize}${if (sizeUnit == SearchFilters.SIZE_UNIT_MB) " МБ" else " ГБ"}"
            else -> "${minSize}-${maxSize}${if (sizeUnit == SearchFilters.SIZE_UNIT_MB) " МБ" else " ГБ"}"
        }
        sizeFilterValue.text = sizeText
        notifyFiltersChanged()
    }
    
    private fun setThemeMode(themeMode: Int) {
        preferencesRepository.setThemeMode(themeMode)
        AppCompatDelegate.setDefaultNightMode(themeMode)
        (activity as? ThemeChangeListener)?.onThemeChanged(themeMode)
    }
    
    private fun setSortMode(sortMode: Int) {
        val sharedPrefs = requireContext().getSharedPreferences("MovieTorrPrefs", 0)
        sharedPrefs.edit().putInt(PREF_SORT_MODE, sortMode).apply()
    }
    
    private fun notifyFiltersChanged() {
        (activity as? FiltersChangeListener)?.onFiltersChanged()
    }
    
    private fun showDateFilterDialog(dateFilterValue: TextView) {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_date_filter, null)
        builder.setView(dialogView)
        
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroupDateFilter)
        
        // Устанавливаем текущее значение
        val currentDateFilterMode = preferencesRepository.getDateFilterMode()
        when (currentDateFilterMode) {
            SearchFilters.DATE_FILTER_DAY -> dialogView.findViewById<RadioButton>(R.id.radioDateDay).isChecked = true
            SearchFilters.DATE_FILTER_WEEK -> dialogView.findViewById<RadioButton>(R.id.radioDateWeek).isChecked = true
            SearchFilters.DATE_FILTER_MONTH -> dialogView.findViewById<RadioButton>(R.id.radioDateMonth).isChecked = true
            SearchFilters.DATE_FILTER_YEAR -> dialogView.findViewById<RadioButton>(R.id.radioDateYear).isChecked = true
            else -> dialogView.findViewById<RadioButton>(R.id.radioDateOff).isChecked = true
        }
        
        builder.setPositiveButton("Применить") { dialog, _ ->
            val selectedDateFilterMode = when (radioGroup.checkedRadioButtonId) {
                R.id.radioDateDay -> SearchFilters.DATE_FILTER_DAY
                R.id.radioDateWeek -> SearchFilters.DATE_FILTER_WEEK
                R.id.radioDateMonth -> SearchFilters.DATE_FILTER_MONTH
                R.id.radioDateYear -> SearchFilters.DATE_FILTER_YEAR
                else -> SearchFilters.DATE_FILTER_OFF
            }
            
            preferencesRepository.setDateFilterMode(selectedDateFilterMode)
            
            val dateFilterText = when (selectedDateFilterMode) {
                SearchFilters.DATE_FILTER_DAY -> getString(R.string.filter_date_day)
                SearchFilters.DATE_FILTER_WEEK -> getString(R.string.filter_date_week)
                SearchFilters.DATE_FILTER_MONTH -> getString(R.string.filter_date_month)
                SearchFilters.DATE_FILTER_YEAR -> getString(R.string.filter_date_year)
                else -> getString(R.string.filter_date_off)
            }
            dateFilterValue.text = dateFilterText
            
            // Уведомляем об изменении фильтров
            notifyFiltersChanged()
            
            dialog.dismiss()
        }
        
        builder.setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
        
        builder.show()
    }
    
    private fun showLegalInfo() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.settings_legal_info))
            .setMessage(getString(R.string.settings_legal_message))
            .setPositiveButton(getString(R.string.settings_legal_ok), null)
            .show()
    }
}