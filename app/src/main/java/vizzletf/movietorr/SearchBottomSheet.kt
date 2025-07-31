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
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import android.widget.TextView
import android.widget.Spinner

class SearchBottomSheet : BottomSheetDialogFragment() {
    private lateinit var torApiService: TorApiService
    private var initialQuery: String? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var statusText: TextView
    private lateinit var filterSpinner: Spinner
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
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_search, container, false)
        
        val searchEdit = view.findViewById<TextInputEditText>(R.id.editSearch)
        val searchInputLayout = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.searchInputLayout)
        // val progressBar = view.findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.progressBar)
        recyclerView = view.findViewById(R.id.recyclerView)
        statusText = view.findViewById(R.id.statusText)
        filterSpinner = view.findViewById(R.id.filterSpinner)
        
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
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(context)
        torrentAdapter = TorrentAdapter()
        recyclerView.adapter = torrentAdapter
    }

    private fun setupFilter() {
        // Создаем адаптер для Spinner
        val adapter = android.widget.ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            mutableListOf(getString(R.string.search_all_categories))
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        filterSpinner.adapter = adapter
        filterSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    selectedCategory = null
                } else {
                    selectedCategory = parent?.getItemAtPosition(position) as? String
                }
                applyFilter()
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                selectedCategory = null
                applyFilter()
            }
        }
    }

    private fun applyFilter() {
        filteredTorrents.clear()
        if (selectedCategory == null) {
            filteredTorrents.addAll(allTorrents)
        } else {
            filteredTorrents.addAll(allTorrents.filter { 
                it.item.Category == selectedCategory
            })
        }
        torrentAdapter.updateTorrents(filteredTorrents)
        updateStatusText()
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
            val adapter = (filterSpinner.adapter as? android.widget.ArrayAdapter<String>) ?: return
            adapter.clear()
            adapter.add(getString(R.string.search_all_categories))
            adapter.addAll(categories)
            adapter.notifyDataSetChanged()
        }
    }

    private fun updateStatusText() {
        val ruTrackerCount = filteredTorrents.count { it.source == "RuTracker" }
        val kinozalCount = filteredTorrents.count { it.source == "Kinozal" }
        val ruTorCount = filteredTorrents.count { it.source == "RuTor" }
        val noNameClubCount = filteredTorrents.count { it.source == "NoNameClub" }
        
        statusText.text = getString(R.string.search_found, filteredTorrents.size) +
                         "(RuTracker: $ruTrackerCount, Kinozal: $kinozalCount, " +
                         "RuTor: $ruTorCount, NoName-Club: $noNameClubCount)"
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
                .inflate(R.layout.item_torrent_simple, parent, false)
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
                val adapter = (filterSpinner.adapter as? android.widget.ArrayAdapter<String>) ?: return
                val position = adapter.getPosition(category)
                if (position != -1) {
                    filterSpinner.setSelection(position)
                }
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