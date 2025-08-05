package vizzletf.movietorr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import android.widget.TextView
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.RadioButton
import vizzletf.movietorr.data.PreferencesRepository
import android.graphics.Canvas
import android.graphics.Rect
import androidx.core.content.ContextCompat
import vizzletf.movietorr.SettingsBottomSheet.FiltersChangeListener

class SearchBottomSheet : BottomSheetDialogFragment(), FiltersChangeListener {
    private lateinit var torApiService: TorApiService
    private lateinit var preferencesRepository: PreferencesRepository
    private var initialQuery: String? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var torrentAdapter: TorrentAdapter
    private var allTorrents = mutableListOf<TorrentItemWithSource>()
    private var filteredTorrents = mutableListOf<TorrentItemWithSource>()
    private var availableCategories = mutableSetOf<String>()
    private var selectedCategory: String? = null
    
    // Локальные фильтры для текущего поиска (не изменяют глобальные настройки)
    private var localSizeFilterMin: Int = 0
    private var localSizeFilterMax: Int = 0
    private var localSizeFilterUnit: String = "MB"
    private var localMinSeeds: Int = 0
    private var localDateFilterMode: Int = SearchFilters.DATE_FILTER_OFF
    private var localSelectedCategory: String = "all"
    private var localSortMode: Int = SearchFilters.SORT_DEFAULT
    
    // UI элементы для дополнительных фильтров
    private lateinit var filtersToggleContainer: LinearLayout
    private lateinit var filtersToggleIcon: android.widget.ImageView
    private lateinit var advancedFiltersContainer: LinearLayout
    private lateinit var sizeFilterContainer: LinearLayout
    private lateinit var minSeedsContainer: LinearLayout
    private lateinit var dateFilterContainer: LinearLayout
    private lateinit var categoryFilterContainer: LinearLayout
    private lateinit var sizeFilterValue: TextView
    private lateinit var minSeedsValue: TextView
    private lateinit var dateFilterValue: TextView
    private lateinit var categoryFilterValue: TextView

    private lateinit var sortFilterContainer: LinearLayout
    private lateinit var sortFilterValue: TextView
    
    private var filtersExpanded = false

    companion object {
        private const val ARG_QUERY = "query"
        fun newInstance(query: String): SearchBottomSheet {
            val fragment = SearchBottomSheet()
            val args = Bundle()
            args.putString(ARG_QUERY, query)
            fragment.arguments = args
            return fragment
        }
    }

    data class TorrentItemWithSource(
        val item: TorApiService.TorrentItem,
        val source: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialQuery = arguments?.getString(ARG_QUERY)
        torApiService = TorApiService()
        preferencesRepository = PreferencesRepository(requireContext())
        loadLocalFiltersFromPreferences()
    }
    
    private fun loadLocalFiltersFromPreferences() {
        // Load settings from main preferences to local filters
        localSizeFilterMin = preferencesRepository.getSizeFilterMin()
        localSizeFilterMax = preferencesRepository.getSizeFilterMax()
        localSizeFilterUnit = preferencesRepository.getSizeFilterUnit()
        localMinSeeds = preferencesRepository.getMinSeeds()
        localDateFilterMode = preferencesRepository.getDateFilterMode()
        localSortMode = preferencesRepository.getSortMode()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_search_settings_style, container, false)
        
        val searchEdit = view.findViewById<TextInputEditText>(R.id.editSearch)
        val searchInputLayout = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.searchInputLayout)
        // val progressBar = view.findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.progressBar)
        recyclerView = view.findViewById(R.id.recyclerView)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        
        // Инициализация элементов дополнительных фильтров
        filtersToggleContainer = view.findViewById(R.id.filtersToggleContainer)
        filtersToggleIcon = view.findViewById(R.id.filtersToggleIcon)
        advancedFiltersContainer = view.findViewById(R.id.advancedFiltersContainer)
        sizeFilterContainer = view.findViewById(R.id.sizeFilterContainer)
        minSeedsContainer = view.findViewById(R.id.minSeedsContainer)
        dateFilterContainer = view.findViewById(R.id.dateFilterContainer)
        categoryFilterContainer = view.findViewById(R.id.categoryFilterContainer)
        sortFilterContainer = view.findViewById(R.id.sortFilterContainer)
        sizeFilterValue = view.findViewById(R.id.sizeFilterValue)
        minSeedsValue = view.findViewById(R.id.minSeedsValue)
        dateFilterValue = view.findViewById(R.id.dateFilterValue)
        categoryFilterValue = view.findViewById(R.id.categoryFilterValue)
        sortFilterValue = view.findViewById(R.id.sortFilterValue)
        
        setupRecyclerView()
        setupAdvancedFilters()
        setupFiltersToggle()
        updateAdvancedFiltersVisibility()
        updateFilterValues()
        
        if (!initialQuery.isNullOrBlank()) {
            searchEdit.setText(initialQuery)
            searchTorrents(initialQuery!!)
        }
        
        // Обработка нажатия Enter в поле поиска
        searchEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = searchEdit.text.toString()
                if (query.isNotBlank()) {
                    searchTorrents(query)
                } else {
                    Toast.makeText(context, getString(R.string.search_enter_movie), Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }
        
        // Обработка клика по иконке лупы
        searchInputLayout.setEndIconOnClickListener {
            val query = searchEdit.text.toString()
            if (query.isNotBlank()) {
                searchTorrents(query)
            } else {
                Toast.makeText(context, getString(R.string.search_enter_movie), Toast.LENGTH_SHORT).show()
            }
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
            
            // Настраиваем правильное поведение скроллинга
            behavior.addBottomSheetCallback(object : com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    // Обработка изменений состояния
                }
                
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    // Обработка скольжения
                }
            })
            
            // Настраиваем поведение скроллинга для RecyclerView
            behavior.isDraggable = true
            behavior.setHideable(true)
            behavior.setSkipCollapsed(false)
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(context)
        torrentAdapter = TorrentAdapter()
        recyclerView.adapter = torrentAdapter
        
        // Настраиваем правильное поведение скроллинга
        recyclerView.isNestedScrollingEnabled = true
        recyclerView.overScrollMode = View.OVER_SCROLL_NEVER
        
        // Добавляем разделители в стиле настроек
        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val position = parent.getChildAdapterPosition(view)
                if (position < parent.adapter?.itemCount ?: 0 - 1) {
                    // Добавляем отступ снизу для разделителя
                    outRect.bottom = 1
                }
            }
            
            override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                val divider = ContextCompat.getDrawable(parent.context, R.drawable.settings_divider_ios)
                val leftMargin = parent.context.resources.getDimensionPixelSize(R.dimen.divider_margin_start)
                val rightMargin = parent.context.resources.getDimensionPixelSize(R.dimen.divider_margin_end)
                
                for (i in 0 until parent.childCount - 1) {
                    val child = parent.getChildAt(i)
                    val position = parent.getChildAdapterPosition(child)
                    
                    if (position < (parent.adapter?.itemCount ?: 0) - 1) {
                        val params = child.layoutParams as RecyclerView.LayoutParams
                        val top = child.bottom + params.bottomMargin
                        val bottom = top + 1
                        
                        divider?.setBounds(
                            child.left + leftMargin,
                            top,
                            child.right - rightMargin,
                            bottom
                        )
                        divider?.draw(c)
                    }
                }
            }
        })
        
        // Добавляем слушатель прокрутки для скрытия фильтров
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && filtersExpanded) {
                    // Скрываем фильтры при прокрутке вниз
                    filtersExpanded = false
                    updateFiltersVisibility()
                }
            }
        })
    }

    private fun setupFiltersToggle() {
        filtersToggleContainer.setOnClickListener {
            filtersExpanded = !filtersExpanded
            updateFiltersVisibility()
        }
        
        // Изначально фильтры скрыты
        updateFiltersVisibility()
    }
    
    private fun updateFiltersVisibility() {
        val toggleText = filtersToggleContainer.findViewById<TextView>(R.id.filtersToggleText)
        
        if (filtersExpanded) {
            // Show filters with animation
            advancedFiltersContainer.visibility = View.VISIBLE
            advancedFiltersContainer.alpha = 0f
            advancedFiltersContainer.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
            
            // Rotate arrow and update text
            filtersToggleIcon.animate()
                .rotation(180f)
                .setDuration(300)
                .start()
            
            toggleText?.text = "Скрыть"
        } else {
            // Hide filters with animation
            advancedFiltersContainer.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    advancedFiltersContainer.visibility = View.GONE
                }
                .start()
            
            // Rotate arrow back and update text
            filtersToggleIcon.animate()
                .rotation(0f)
                .setDuration(300)
                .start()
            
            toggleText?.text = "Показать"
        }
    }

    private fun setupAdvancedFilters() {
        // Обработчики кликов для дополнительных фильтров
        sizeFilterContainer.setOnClickListener {
            showSizeFilterDialog()
        }
        
        minSeedsContainer.setOnClickListener {
            showMinSeedsDialog()
        }
        
        dateFilterContainer.setOnClickListener {
            showDateFilterDialog()
        }
        
        categoryFilterContainer.setOnClickListener {
            showCategoryFilterDialog()
        }

        
        sortFilterContainer.setOnClickListener {
            showSortFilterDialog()
        }
        
        updateFilterValues()
    }
    
    private fun updateAdvancedFiltersVisibility() {
        // Always show advanced filters
        advancedFiltersContainer.visibility = View.VISIBLE
    }
    
    private fun updateFilterValues() {
        // Обновление значений фильтров
        updateSizeFilterValue()
        updateMinSeedsValue()
        updateDateFilterValue()
        updateCategoryFilterValue()
        updateSortFilterValue()
    }
    
    private fun updateSizeFilterValue() {
        val sizeText = when {
            localSizeFilterMin == 0 && localSizeFilterMax == 0 -> "Без ограничений"
            localSizeFilterMin > 0 && localSizeFilterMax == 0 -> "От ${localSizeFilterMin} ${localSizeFilterUnit}"
            localSizeFilterMin == 0 && localSizeFilterMax > 0 -> "До ${localSizeFilterMax} ${localSizeFilterUnit}"
            else -> "${localSizeFilterMin}-${localSizeFilterMax} ${localSizeFilterUnit}"
        }
        sizeFilterValue.text = sizeText
    }
    
    private fun updateMinSeedsValue() {
        val minSeedsText = when (localMinSeeds) {
            0 -> "Без ограничений"
            else -> "Минимум $localMinSeeds"
        }
        minSeedsValue.text = minSeedsText
    }
    
    private fun updateDateFilterValue() {
        val dateFilterText = when (localDateFilterMode) {
            SearchFilters.DATE_FILTER_DAY -> getString(R.string.filter_date_day)
            SearchFilters.DATE_FILTER_WEEK -> getString(R.string.filter_date_week)
            SearchFilters.DATE_FILTER_MONTH -> getString(R.string.filter_date_month)
            SearchFilters.DATE_FILTER_YEAR -> getString(R.string.filter_date_year)
            else -> getString(R.string.filter_date_off)
        }
        dateFilterValue.text = dateFilterText
    }
    
    private fun updateCategoryFilterValue() {
        categoryFilterValue.text = if (localSelectedCategory == "all") {
            "Все категории"
        } else {
            localSelectedCategory
        }
    }
    
    private fun updateSortFilterValue() {
        sortFilterValue.text = when (localSortMode) {
            SearchFilters.SORT_BY_SIZE -> "По размеру"
            SearchFilters.SORT_BY_DATE -> "По дате"
            SearchFilters.SORT_BY_SEEDS -> "По сидам"
            SearchFilters.SORT_BY_NAME -> "По названию"
            else -> "По умолчанию"
        }
    }

    private fun applyFilter() {
        filteredTorrents.clear()
        
        // Always use local filters (advanced mode)
        val searchFilters = SearchFilters(
            enabledTrackers = preferencesRepository.getEnabledTrackers(),
            sizeFilterMin = localSizeFilterMin,
            sizeFilterMax = localSizeFilterMax,
            sizeFilterUnit = localSizeFilterUnit,
            minSeeds = localMinSeeds,
            dateFilterMode = localDateFilterMode
        )
        
        var filteredList = allTorrents.toMutableList()
        
        // Фильтр по трекерам
        if (searchFilters.enabledTrackers.isNotEmpty()) {
            filteredList = filteredList.filter { torrent ->
                searchFilters.enabledTrackers.contains(torrent.source)
            }.toMutableList()
        }
        
        // Фильтр по размеру
        if (searchFilters.isSizeFilterEnabled()) {
            filteredList = filteredList.filter { torrent ->
                val torrentSizeInMB = convertSizeToMB(parseSize(torrent.item.Size), getSizeUnit(torrent.item.Size))
                val minSizeInMB = if (searchFilters.sizeFilterMin > 0) {
                    convertSizeToMB(searchFilters.sizeFilterMin, searchFilters.sizeFilterUnit)
                } else 0
                val maxSizeInMB = if (searchFilters.sizeFilterMax > 0) {
                    convertSizeToMB(searchFilters.sizeFilterMax, searchFilters.sizeFilterUnit)
                } else Int.MAX_VALUE
                
                torrentSizeInMB >= minSizeInMB && torrentSizeInMB <= maxSizeInMB
            }.toMutableList()
        }
        
        // Фильтр по минимальному количеству сидов
        if (searchFilters.isMinSeedsEnabled()) {
            filteredList = filteredList.filter { torrent ->
                torrent.item.Seeds.toIntOrNull() ?: 0 >= searchFilters.minSeeds
            }.toMutableList()
        }
        
        // Фильтр по дате добавления
        if (searchFilters.isDateFilterEnabled()) {
            filteredList = filteredList.filter { torrent ->
                isWithinDateFilter(torrent.item.Date, searchFilters.dateFilterMode)
            }.toMutableList()
        }
        
        // Фильтр по категории
        val categoryToFilter = localSelectedCategory
        if (categoryToFilter == null || categoryToFilter == "all") {
            filteredTorrents.addAll(filteredList)
        } else {
            filteredTorrents.addAll(filteredList.filter { 
                it.item.Category == categoryToFilter
            })
        }
        
        torrentAdapter.updateTorrents(filteredTorrents)
        updateStatusText()
    }
    
    private fun convertSizeToMB(size: Long, unit: String): Int {
        return when (unit.uppercase()) {
            "GB" -> (size / (1024 * 1024)).toInt()
            "MB" -> (size / 1024).toInt()
            "KB" -> (size / (1024 * 1024)).toInt()
            else -> (size / (1024 * 1024)).toInt()
        }
    }
    
    private fun convertSizeToMB(size: Int, unit: String): Int {
        return when (unit.uppercase()) {
            "GB" -> size * 1024
            "MB" -> size
            "KB" -> size / 1024
            else -> size
        }
    }
    
    private fun getSizeUnit(sizeStr: String): String {
        return when {
            sizeStr.contains("GB", ignoreCase = true) -> "GB"
            sizeStr.contains("MB", ignoreCase = true) -> "MB"
            sizeStr.contains("KB", ignoreCase = true) -> "KB"
            else -> "B"
        }
    }

    private fun searchTorrents(query: String) {
        val progressBar = view?.findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.progressBar)
        val searchInputLayout = view?.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.searchInputLayout)
        
        // Показываем анимацию поиска
        progressBar?.visibility = View.VISIBLE
        searchInputLayout?.isEnabled = false
        
        torApiService.searchTorrents(query, object : TorApiService.TorrentSearchCallback {
            override fun onSuccess(response: TorApiService.TorrentResponse) {
                activity?.runOnUiThread {
                    // Проверяем что фрагмент еще прикреплен
                    if (isAdded && !isDetached) {
                        // Скрываем анимацию поиска
                        progressBar?.visibility = View.GONE
                        searchInputLayout?.isEnabled = true
                        displayResults(response)
                    }
                }
            }
            override fun onError(error: String) {
                activity?.runOnUiThread {
                    // Проверяем что фрагмент еще прикреплен
                    if (isAdded && !isDetached) {
                        // Скрываем анимацию поиска
                        progressBar?.visibility = View.GONE
                        searchInputLayout?.isEnabled = true
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun displayResults(response: TorApiService.TorrentResponse) {
        allTorrents.clear()
        availableCategories.clear()
        
        val ruTrackerList = response.getRuTrackerList()
        ruTrackerList.forEach { 
            allTorrents.add(TorrentItemWithSource(it, "RuTracker"))
            it.Category?.let { category -> availableCategories.add(category) }
        }
        
        val kinozalList = response.getKinozalList()
        kinozalList.forEach { 
            allTorrents.add(TorrentItemWithSource(it, "Kinozal"))
            it.Category?.let { category -> availableCategories.add(category) }
        }
        
        val ruTorList = response.getRuTorList()
        ruTorList.forEach { 
            allTorrents.add(TorrentItemWithSource(it, "RuTor"))
            it.Category?.let { category -> availableCategories.add(category) }
        }
        
        val noNameClubList = response.getNoNameClubList()
        noNameClubList.forEach { 
            allTorrents.add(TorrentItemWithSource(it, "NoNameClub"))
            it.Category?.let { category -> availableCategories.add(category) }
        }
        
        // Сортируем торренты согласно настройкам
        sortTorrents()
        
        // Применяем текущий фильтр
        applyFilter()
    }



    private fun updateStatusText() {
        // Показываем или скрываем сообщение "Ничего не найдено"
        emptyStateText.visibility = if (filteredTorrents.isEmpty()) View.VISIBLE else View.GONE
        
        // Показываем или скрываем список результатов
        recyclerView.visibility = if (filteredTorrents.isNotEmpty()) View.VISIBLE else View.GONE
    }
    
    private fun sortTorrents() {
        // Always use local sort mode (advanced mode)
        val sortMode = localSortMode
        
        when (sortMode) {
            SearchFilters.SORT_BY_SIZE -> {
                allTorrents.sortByDescending { parseSize(it.item.Size) }
            }
            SearchFilters.SORT_BY_DATE -> {
                allTorrents.sortByDescending { parseDate(it.item.Date) }
            }
            SearchFilters.SORT_BY_SEEDS -> {
                allTorrents.sortByDescending { it.item.Seeds.toIntOrNull() ?: 0 }
            }
            SearchFilters.SORT_BY_NAME -> {
                allTorrents.sortBy { it.item.Name ?: "" }
            }
            else -> {
                // По умолчанию - оставляем как есть (порядок от API)
            }
        }
    }
    
    private fun parseSize(sizeStr: String): Long {
        return try {
            val size = sizeStr.replace(" ", "").lowercase()
            val multiplier = when {
                size.endsWith("gb") -> 1024 * 1024 * 1024L
                size.endsWith("mb") -> 1024 * 1024L
                size.endsWith("kb") -> 1024L
                else -> 1L
            }
            val number = size.filter { it.isDigit() || it == '.' }.toDouble()
            (number * multiplier).toLong()
        } catch (e: Exception) {
            0L
        }
    }
    
    private fun parseDate(dateStr: String): Long {
        return try {
            // Простая сортировка по строке даты (формат YYYY-MM-DD)
            dateStr.replace("-", "").toLong()
        } catch (e: Exception) {
            0L
        }
    }
    
    private fun isWithinDateFilter(dateStr: String, filterMode: Int): Boolean {
        try {
            // Предполагаем, что дата в формате "dd.MM.yyyy" или "yyyy-MM-dd"
            val date = parseDateForFilter(dateStr) ?: return true // Если не удалось распарсить дату, пропускаем фильтр
            val now = System.currentTimeMillis()
            val diff = now - date
            
            return when (filterMode) {
                SearchFilters.DATE_FILTER_DAY -> diff <= 24 * 60 * 60 * 1000 // 1 день в миллисекундах
                SearchFilters.DATE_FILTER_WEEK -> diff <= 7 * 24 * 60 * 60 * 1000 // 1 неделя
                SearchFilters.DATE_FILTER_MONTH -> diff <= 30 * 24 * 60 * 60 * 1000 // ~1 месяц
                SearchFilters.DATE_FILTER_YEAR -> diff <= 365 * 24 * 60 * 60 * 1000 // ~1 год
                else -> true // Если фильтр не установлен, пропускаем все
            }
        } catch (e: Exception) {
            return true // В случае ошибки пропускаем фильтр
        }
    }
    
    private fun parseDateForFilter(dateStr: String): Long? {
        return try {
            // Пробуем разные форматы даты
            val formats = listOf(
                java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()),
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()),
                java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            )
            
            for (format in formats) {
                try {
                    return format.parse(dateStr)?.time
                } catch (e: Exception) {
                    // Пробуем следующий формат
                }
            }
            
            null // Не удалось распарсить дату
        } catch (e: Exception) {
            null
        }
    }
    
    private fun showSizeFilterDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_size_filter, null)
        builder.setView(dialogView)
        
        val editSizeMin = dialogView.findViewById<TextInputEditText>(R.id.editSizeMin)
        val editSizeMax = dialogView.findViewById<TextInputEditText>(R.id.editSizeMax)
        val spinnerSizeUnit = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerSizeUnit)
        
        // Setup size unit dropdown
        val sizeUnits = arrayOf("MB", "GB")
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sizeUnits)
        spinnerSizeUnit.setAdapter(adapter)
        
        // Set current values
        if (localSizeFilterMin > 0) editSizeMin.setText(localSizeFilterMin.toString())
        if (localSizeFilterMax > 0) editSizeMax.setText(localSizeFilterMax.toString())
        spinnerSizeUnit.setText(localSizeFilterUnit, false)
        
        builder.setPositiveButton("Применить") { dialog, _ ->
            val minSize = editSizeMin.text.toString().toIntOrNull() ?: 0
            val maxSize = editSizeMax.text.toString().toIntOrNull() ?: 0
            val sizeUnit = spinnerSizeUnit.text.toString().ifEmpty { "MB" }
            
            localSizeFilterMin = minSize
            localSizeFilterMax = maxSize
            localSizeFilterUnit = sizeUnit
            
            updateSizeFilterValue()
            applyFilter()
            dialog.dismiss()
        }
        
        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.dismiss()
        }
        
        builder.show()
    }
    
    private fun showMinSeedsDialog() {
        val minSeedsOptions = arrayOf(
            "Без ограничений",
            "Минимум 1",
            "Минимум 10",
            "Минимум 100"
        )
        
        val selectedIndex = when (localMinSeeds) {
            SearchFilters.MIN_SEEDS_1 -> 1
            SearchFilters.MIN_SEEDS_10 -> 2
            SearchFilters.MIN_SEEDS_100 -> 3
            else -> 0
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Минимальное количество сидов")
            .setSingleChoiceItems(minSeedsOptions, selectedIndex) { dialog, which ->
                val newMinSeeds = when (which) {
                    1 -> SearchFilters.MIN_SEEDS_1
                    2 -> SearchFilters.MIN_SEEDS_10
                    3 -> SearchFilters.MIN_SEEDS_100
                    else -> SearchFilters.MIN_SEEDS_OFF
                }
                
                localMinSeeds = newMinSeeds
                updateMinSeedsValue()
                applyFilter()
                dialog.dismiss()
            }
            .show()
    }
    
    private fun showDateFilterDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_date_filter, null)
        builder.setView(dialogView)
        
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroupDateFilter)
        
        // Устанавливаем текущее значение
        when (localDateFilterMode) {
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
            
            localDateFilterMode = selectedDateFilterMode
            updateDateFilterValue()
            applyFilter()
            dialog.dismiss()
        }
        
        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.dismiss()
        }
        
        builder.show()
    }
    
    private fun showCategoryFilterDialog() {
        val categories = mutableListOf<Pair<String, String>>()
        categories.add("all" to "Все категории")
        
        // Add available categories from torrents
        availableCategories.sorted().forEach { category ->
            categories.add(category to category)
        }
        
        val categoryNames = categories.map { it.second }.toTypedArray()
        val selectedIndex = categories.indexOfFirst { it.first == localSelectedCategory }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Выберите категорию")
            .setSingleChoiceItems(categoryNames, selectedIndex) { dialog, which ->
                val selectedCategoryPair = categories[which]
                localSelectedCategory = selectedCategoryPair.first
                updateCategoryFilterValue()
                applyFilter()
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun showSortFilterDialog() {
        val sortModes = listOf(
            SearchFilters.SORT_DEFAULT,
            SearchFilters.SORT_BY_SIZE,
            SearchFilters.SORT_BY_DATE,
            SearchFilters.SORT_BY_SEEDS,
            SearchFilters.SORT_BY_NAME
        )
        val sortNames = listOf("По умолчанию", "По размеру", "По дате", "По сидам", "По названию")
        
        val selectedIndex = sortModes.indexOf(localSortMode)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Выберите сортировку")
            .setSingleChoiceItems(sortNames.toTypedArray(), selectedIndex) { dialog, which ->
                val selectedSort = sortModes[which]
                localSortMode = selectedSort
                updateSortFilterValue()
                sortTorrents()
                applyFilter()
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onFiltersChanged() {
        // Reload local filters from main preferences when settings change
        loadLocalFiltersFromPreferences()
        updateAdvancedFiltersVisibility()
        updateFilterValues()
        sortTorrents()
        applyFilter()
    }
    
    private fun getMagnetLink(source: String, id: String) {
        torApiService.getMagnetLink(source, id, object : TorApiService.MagnetCallback {
            override fun onSuccess(magnet: String, hash: String) {
                activity?.runOnUiThread {
                    // Проверяем что фрагмент еще прикреплен
                    if (isAdded && !isDetached) {
                        showMagnetOptions(magnet, hash)
                    }
                }
            }
            
            override fun onError(error: String) {
                activity?.runOnUiThread {
                    // Проверяем что фрагмент еще прикреплен
                    if (isAdded && !isDetached) {
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
    
    private fun showMagnetOptions(magnet: String, hash: String) {
        context?.let { ctx ->
            MaterialAlertDialogBuilder(ctx)
                .setTitle("Magnet Link")
                .setItems(arrayOf("Open", "Copy Hash")) { _, which ->
                    when (which) {
                        0 -> {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(magnet))
                            ctx.startActivity(intent)
                        }
                        1 -> {
                            val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Torrent Hash", hash)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(ctx, "Hash copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
                .show()
        }
    }
    
    inner class TorrentAdapter : RecyclerView.Adapter<TorrentAdapter.TorrentViewHolder>() {
        
        private var torrents = listOf<TorrentItemWithSource>()
        
        fun updateTorrents(newTorrents: List<TorrentItemWithSource>) {
            torrents = newTorrents
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TorrentViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_torrent_settings_style, parent, false)
            return TorrentViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: TorrentViewHolder, position: Int) {
            holder.bind(torrents[position])
        }
        
        override fun getItemCount() = torrents.size
        
        inner class TorrentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val sourceText: TextView = itemView.findViewById(R.id.sourceText)
            private val nameText: TextView = itemView.findViewById(R.id.nameText)
            private val categoryText: TextView = itemView.findViewById(R.id.categoryText)
            private val sizeText: TextView = itemView.findViewById(R.id.sizeText)
            private val seedsText: TextView = itemView.findViewById(R.id.seedsText)
            private val peersText: TextView = itemView.findViewById(R.id.peersText)
            private val dateText: TextView = itemView.findViewById(R.id.dateText)

            fun bind(torrentWithSource: TorrentItemWithSource) {
                val torrent = torrentWithSource.item
                val source = torrentWithSource.source

                sourceText.text = source
                nameText.text = torrent.Name
                categoryText.text = torrent.Category ?: getString(R.string.search_no_category)
                sizeText.text = torrent.Size
                seedsText.text = "${torrent.Seeds}"
                peersText.text = "${torrent.Peers}"
                dateText.text = torrent.Date.split(' ')[0]

                // Клик по источнику открывает страницу торрента в приложении
                sourceText.setOnClickListener {
                    (activity as? MainActivity)?.loadSite(torrent.Url)
                    dismiss()
                }

                // Клик по категории автоматически устанавливает фильтр
                categoryText.setOnClickListener {
                    val category = torrent.Category
                    if (category != null) {
                        selectedCategory = category
                        localSelectedCategory = category
                        updateCategoryFilterValue()
                        applyFilter()
                    }
                }

                // Делаем всю карточку кликабельной для магнитной ссылки
                itemView.setOnClickListener {
                    this@SearchBottomSheet.getMagnetLink(source, torrent.Id)
                }
            }
        }
    }
}