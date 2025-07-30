package com.movietorr.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SearchHistoryFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var clearButton: FloatingActionButton
    private lateinit var historyAdapter: SearchHistoryAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search_history, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerView = view.findViewById(R.id.historyRecyclerView)
        emptyText = view.findViewById(R.id.emptyText)
        clearButton = view.findViewById(R.id.clearHistoryButton)
        
        setupRecyclerView()
        setupClearButton()
        loadHistory()
    }
    
    private fun setupRecyclerView() {
        historyAdapter = SearchHistoryAdapter { searchQuery ->
            // Открываем поиск с выбранным запросом
            (activity as? MainActivity)?.openTorrentSearch(searchQuery, "")
        }
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = historyAdapter
    }
    
    private fun setupClearButton() {
        clearButton.setOnClickListener {
            SearchHistoryManager.clearHistory(requireContext())
            loadHistory()
        }
    }
    
    private fun loadHistory() {
        val history = SearchHistoryManager.getHistory(requireContext())
        if (history.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyText.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyText.visibility = View.GONE
            historyAdapter.updateHistory(history)
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadHistory()
    }
    
    class SearchHistoryAdapter(
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder>() {
        
        private var history = listOf<SearchHistoryItem>()
        
        fun updateHistory(newHistory: List<SearchHistoryItem>) {
            history = newHistory
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = history[position]
            holder.bind(item)
        }
        
        override fun getItemCount() = history.size
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val textView: TextView = itemView.findViewById(android.R.id.text1)
            
            fun bind(item: SearchHistoryItem) {
                textView.text = "${item.query} (${item.date})"
                itemView.setOnClickListener {
                    onItemClick(item.query)
                }
            }
        }
    }
} 