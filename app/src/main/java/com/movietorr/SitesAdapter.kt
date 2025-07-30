package com.movietorr

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class SitesAdapter(
    private val onSiteClick: (SiteConfig) -> Unit,
    private val onSiteDelete: (SiteConfig) -> Unit
) : RecyclerView.Adapter<SitesAdapter.SiteViewHolder>() {
    
    private var sites: List<SiteConfig> = emptyList()
    
    fun updateSites(newSites: List<SiteConfig>) {
        sites = newSites
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_site, parent, false)
        return SiteViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: SiteViewHolder, position: Int) {
        holder.bind(sites[position])
    }
    
    override fun getItemCount(): Int = sites.size
    
    inner class SiteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.textSiteName)
        private val urlText: TextView = itemView.findViewById(R.id.textSiteUrl)
        private val deleteButton: MaterialButton = itemView.findViewById(R.id.btnDeleteSite)
        
        fun bind(site: SiteConfig) {
            nameText.text = site.name
            urlText.text = site.url
            
            itemView.setOnClickListener { onSiteClick(site) }
            
            deleteButton.visibility = View.VISIBLE
            deleteButton.setOnClickListener { onSiteDelete(site) }
        }
    }
} 