package com.movietorr.v2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class SearchBottomSheet : BottomSheetDialogFragment() {
    private lateinit var torrentService: TorrentService
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_search, container, false)
        
        torrentService = TorrentService()
        
        val searchEdit = view.findViewById<TextInputEditText>(R.id.editSearch)
        val searchButton = view.findViewById<MaterialButton>(R.id.btnSearch)
        
        searchButton.setOnClickListener {
            val query = searchEdit.text.toString()
            if (query.isNotBlank()) {
                searchTorrents(query)
            } else {
                Toast.makeText(context, "Введите название фильма", Toast.LENGTH_SHORT).show()
            }
        }
        
        return view
    }
    
    private fun searchTorrents(query: String) {
        val searchButton = view?.findViewById<MaterialButton>(R.id.btnSearch)
        searchButton?.isEnabled = false
        searchButton?.text = "Поиск..."
        
        torrentService.searchTorrents(query) { torrents, error ->
            requireActivity().runOnUiThread {
                searchButton?.isEnabled = true
                searchButton?.text = "Искать"
                
                if (error != null) {
                    Toast.makeText(context, "Ошибка поиска: $error", Toast.LENGTH_LONG).show()
                } else if (torrents != null) {
                    if (torrents.isEmpty()) {
                        Toast.makeText(context, "Ничего не найдено", Toast.LENGTH_SHORT).show()
                    } else {
                        showSearchResults(torrents)
                    }
                }
            }
        }
    }
    
    private fun showSearchResults(torrents: List<TorrentService.TorrentItem>) {
        // Здесь можно показать результаты поиска в новом BottomSheet
        // Пока просто показываем количество найденных торрентов
        Toast.makeText(context, "Найдено ${torrents.size} торрентов", Toast.LENGTH_SHORT).show()
        dismiss()
    }
}