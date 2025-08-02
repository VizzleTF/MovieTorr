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
import vizzletf.movietorr.data.PreferencesRepository
import android.graphics.Canvas
import android.graphics.Rect
import androidx.core.content.ContextCompat

class SearchBottomSheet : BottomSheetDialogFragment() {
    private lateinit var torApiService: TorApiService
    private lateinit var preferencesRepository: PreferencesRepository
    private var initialQuery: String? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var filterAutoComplete: AutoCompleteTextView
    private lateinit var torrentAdapter: TorrentAdapter
    private var allTorrents = mutableListOf<TorrentItemWithSource>()
    private var filteredTorrents = mutableListOf<TorrentItemWithSource>()
    private var availableCategories = mutableSetOf<String>()
    private var selectedCategory: String? = null

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
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_search_settings_style, container, false)
        
        val searchEdit = view.findViewById<TextInputEditText>(R.id.editSearch)
        val searchInputLayout = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.searchInputLayout)
        // val progressBar = view.findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.progressBar)
        recyclerView = view.findViewById(R.id.recyclerView)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        filterAutoComplete = view.findViewById(R.id.filterAutoComplete)
        
        setupRecyclerView()
        setupFilter()
        
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
    }

    private fun setupFilter() {
        // Создаем адаптер для AutoCompleteTextView с новым стилем iOS 18
        val adapter = android.widget.ArrayAdapter<String>(
            requireContext(),
            R.layout.item_dropdown_category,
            mutableListOf(getString(R.string.search_all_categories))
        )
        
        filterAutoComplete.setAdapter(adapter)
        filterAutoComplete.setText(getString(R.string.search_all_categories), false)
        
        // Настраиваем показ выпадающего списка при клике
        filterAutoComplete.setOnClickListener {
            filterAutoComplete.showDropDown()
        }
        
        filterAutoComplete.setOnItemClickListener { parent, view, position, id ->
            if (position == 0) {
                selectedCategory = null
            } else {
                selectedCategory = filterAutoComplete.text.toString()
            }
            applyFilter()
        }
    }

    private fun applyFilter() {
        filteredTorrents.clear()
        
        // Применяем фильтры из настроек
        val searchFilters = SearchFilters(
            enabledTrackers = preferencesRepository.getEnabledTrackers(),
            sizeFilterMin = preferencesRepository.getSizeFilterMin(),
            sizeFilterMax = preferencesRepository.getSizeFilterMax(),
            sizeFilterUnit = preferencesRepository.getSizeFilterUnit(),
            searchMode = preferencesRepository.getSearchMode(),
            minSeeds = preferencesRepository.getMinSeeds()
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
        
        // Фильтр по категории
        if (selectedCategory == null) {
            filteredTorrents.addAll(filteredList)
        } else {
            filteredTorrents.addAll(filteredList.filter { 
                it.item.Category == selectedCategory
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
        
        // Обновляем подсказку фильтра
        updateFilterHint()
    }

    private fun updateFilterHint() {
        val categories = availableCategories.toList().sorted()
        if (categories.isNotEmpty()) {
            val adapter = android.widget.ArrayAdapter<String>(
                requireContext(),
                R.layout.item_dropdown_category,
                mutableListOf(getString(R.string.search_all_categories))
            )
            adapter.addAll(categories)
            filterAutoComplete.setAdapter(adapter)
        }
    }

    private fun updateStatusText() {
        // Показываем или скрываем сообщение "Ничего не найдено"
        emptyStateText.visibility = if (filteredTorrents.isEmpty()) View.VISIBLE else View.GONE
        
        // Показываем или скрываем список результатов
        recyclerView.visibility = if (filteredTorrents.isNotEmpty()) View.VISIBLE else View.GONE
    }
    
    private fun sortTorrents() {
        val sharedPrefs = requireContext().getSharedPreferences("MovieTorrPrefs", 0)
        val sortMode = sharedPrefs.getInt("sort_mode", 0)
        
        when (sortMode) {
            SettingsBottomSheet.SORT_SIZE -> {
                allTorrents.sortByDescending { parseSize(it.item.Size) }
            }
            SettingsBottomSheet.SORT_DATE -> {
                allTorrents.sortByDescending { parseDate(it.item.Date) }
            }
            SettingsBottomSheet.SORT_SEEDS -> {
                allTorrents.sortByDescending { it.item.Seeds.toIntOrNull() ?: 0 }
            }
            SettingsBottomSheet.SORT_TRACKER -> {
                allTorrents.sortBy { it.source }
            }
            SettingsBottomSheet.SORT_CATEGORY -> {
                allTorrents.sortBy { it.item.Category ?: "" }
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
            val number = size.replace(Regex("[^0-9.]"), "").toDouble()
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
                        setFilterCategory(category)
                    }
                }

                // Делаем всю карточку кликабельной для магнитной ссылки
                itemView.setOnClickListener {
                    getMagnetLink(source, torrent.Id)
                }
            }

            private fun setFilterCategory(category: String) {
                filterAutoComplete.setText(category, false)
                selectedCategory = category
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
                        .setTitle(getString(R.string.magnet_title))
                        .setItems(arrayOf(getString(R.string.magnet_open), getString(R.string.magnet_copy_hash))) { _, which ->
                            when (which) {
                                0 -> {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(magnet))
                                    ctx.startActivity(intent)
                                }
                                1 -> {
                                    val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Torrent Hash", hash)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(ctx, getString(R.string.magnet_hash_copied), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .show()
                }
            }
        }
    }
} 