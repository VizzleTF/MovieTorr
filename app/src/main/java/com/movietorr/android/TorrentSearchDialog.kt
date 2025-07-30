package com.movietorr.android

import androidx.appcompat.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TorrentSearchDialog(
    private val context: Context,
    private val torApiService: TorApiService,
    private val presetTitle: String = "",
    private val presetYear: String = "",
    private val autoSearch: Boolean = false
) {
    
    private lateinit var dialog: AlertDialog
    private lateinit var searchInput: TextInputEditText
    private lateinit var searchButton: MaterialButton
    private lateinit var statusText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: LinearProgressIndicator
    
    private val torrentAdapter = TorrentAdapter()
    private var allTorrents = mutableListOf<TorrentItemWithSource>()
    
    data class TorrentItemWithSource(
        val item: TorApiService.TorrentItem,
        val source: String
    )
    
    fun show() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_torrent_search, null)
        initializeViews(dialogView)
        setupDialog(dialogView)
        setupRecyclerView()
        setupListeners()

        // Предзаполняем поле поиска если есть данные
        if (presetTitle.isNotEmpty()) {
            searchInput.setText("$presetTitle $presetYear".trim())
        }
        
        dialog.show()
        
        // Устанавливаем ширину диалога на всю ширину экрана
        val displayMetrics = context.resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.95).toInt() // 95% ширины экрана
        dialog.window?.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        
        // Центрируем диалог
        dialog.window?.setGravity(android.view.Gravity.CENTER)
        
        // Применяем прозрачность к корневому view
        dialogView.findViewById<View>(R.id.rootCardView)?.alpha = 0.9f
        
        // Автоматически запускаем поиск если включен
        if (autoSearch && presetTitle.isNotEmpty()) {
            // Увеличиваем задержку для полной инициализации диалога
            dialogView.postDelayed({
                if (searchInput.text?.isNotEmpty() == true) {
                    performSearch(false) // Используем обычный поиск по умолчанию
                }
            }, 500)
        }
    }
    
    private fun initializeViews(view: View) {
        searchInput = view.findViewById(R.id.searchInput)
        searchButton = view.findViewById(R.id.searchButton)
        statusText = view.findViewById(R.id.statusText)
        recyclerView = view.findViewById(R.id.recyclerView)
        progressBar = view.findViewById(R.id.progressBar)
    }
    
    private fun setupDialog(view: View) {
        dialog = MaterialAlertDialogBuilder(context)
            .setView(view)
            .setCancelable(true)
            .create()
    }
    
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = torrentAdapter
    }
    
    private fun setupListeners() {
        searchButton.setOnClickListener { performSearch(false) }
    }
    
    private fun performSearch(searchAll: Boolean) {
        val query = searchInput.text?.toString()?.trim() ?: ""
        if (query.isEmpty()) {
            Toast.makeText(context, "Введите поисковый запрос", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Добавляем в историю поиска
        SearchHistoryManager.addToHistory(context, query)
        
        showLoading(true)
        val callback = object : TorApiService.TorrentSearchCallback {
            override fun onSuccess(response: TorApiService.TorrentResponse) {
                (context as MainActivity).runOnUiThread {
                    showLoading(false)
                    displayResults(response)
                }
            }
            
            override fun onError(error: String) {
                (context as MainActivity).runOnUiThread {
                    showLoading(false)
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                }
            }
        }
        
        torApiService.searchTorrents(query, callback)
    }
    
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        searchButton.isEnabled = !show
    }
    
    private fun displayResults(response: TorApiService.TorrentResponse) {
        allTorrents.clear()
        
        val ruTrackerList = response.getRuTrackerList()
        println("Processing RuTracker: ${ruTrackerList.size} items")
        ruTrackerList.forEach { allTorrents.add(TorrentItemWithSource(it, "RuTracker")) }
        
        val kinozalList = response.getKinozalList()
        println("Processing Kinozal: ${kinozalList.size} items")
        kinozalList.forEach { allTorrents.add(TorrentItemWithSource(it, "Kinozal")) }
        
        val ruTorList = response.getRuTorList()
        println("Processing RuTor: ${ruTorList.size} items")
        ruTorList.forEach { allTorrents.add(TorrentItemWithSource(it, "RuTor")) }
        
        val noNameClubList = response.getNoNameClubList()
        println("Processing NoNameClub: ${noNameClubList.size} items")
        noNameClubList.forEach { allTorrents.add(TorrentItemWithSource(it, "NoNameClub")) }
        
        println("Total torrents found: ${allTorrents.size}")
        
        torrentAdapter.updateTorrents(allTorrents)
        updateStatusText()
    }
    

    
    private fun updateStatusText() {
        val ruTrackerCount = allTorrents.count { it.source == "RuTracker" }
        val kinozalCount = allTorrents.count { it.source == "Kinozal" }
        val ruTorCount = allTorrents.count { it.source == "RuTor" }
        val noNameClubCount = allTorrents.count { it.source == "NoNameClub" }
        
        statusText.text = "Найдено: ${allTorrents.size} " +
                         "(RuTracker: $ruTrackerCount, Kinozal: $kinozalCount, " +
                         "RuTor: $ruTorCount, NoName-Club: $noNameClubCount)"
    }
    
    inner class TorrentAdapter : RecyclerView.Adapter<TorrentAdapter.TorrentViewHolder>() {
        
        private var torrents = listOf<TorrentItemWithSource>()
        
        fun updateTorrents(newTorrents: List<TorrentItemWithSource>) {
            torrents = newTorrents
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TorrentViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_torrent, parent, false)
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
                categoryText.text = torrent.Category ?: "Без категории"
                sizeText.text = torrent.Size
                seedsText.text = "${torrent.Seeds}"
                peersText.text = "${torrent.Peers}"
                dateText.text = torrent.Date.split(' ')[0]
                
                // Клик по источнику открывает страницу торрента
                sourceText.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(torrent.Url))
                    context.startActivity(intent)
                }
                
                // Делаем всю карточку кликабельной для магнитной ссылки
                itemView.setOnClickListener {
                    getMagnetLink(source, torrent.Id)
                }
            }
            
            private fun getMagnetLink(source: String, id: String) {
                torApiService.getMagnetLink(source, id, object : TorApiService.MagnetCallback {
                    override fun onSuccess(magnet: String, hash: String) {
                        (context as MainActivity).runOnUiThread {
                            showMagnetOptions(magnet, hash)
                        }
                    }
                    
                    override fun onError(error: String) {
                        (context as MainActivity).runOnUiThread {
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    }
                })
            }
            
            private fun showMagnetOptions(magnet: String, hash: String) {
                AlertDialog.Builder(context)
                    .setTitle("Выберите действие")
                    .setItems(arrayOf("Открыть магнитную ссылку", "Скопировать хеш")) { _, which ->
                        when (which) {
                            0 -> {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(magnet))
                                context.startActivity(intent)
                            }
                            1 -> {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Torrent Hash", hash)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Хеш скопирован в буфер обмена", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .show()
            }
        }
    }
} 