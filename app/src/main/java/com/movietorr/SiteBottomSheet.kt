package com.movietorr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SiteBottomSheet : BottomSheetDialogFragment() {
    private lateinit var sitesAdapter: SitesAdapter
    private lateinit var recyclerView: RecyclerView
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_site, container, false)
        
        recyclerView = view.findViewById(R.id.recyclerSites)
        setupAdapter()
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = sitesAdapter
        
        view.findViewById<MaterialButton>(R.id.btnAddSite).setOnClickListener {
            showAddSiteDialog()
        }
        
        loadSites()
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
    
    private fun loadSites() {
        val sites = SiteManager.getEnabledSites(requireContext())
        sitesAdapter.updateSites(sites)
    }
    
    private fun setupAdapter() {
        sitesAdapter = SitesAdapter(
            onSiteClick = { site ->
                (activity as? MainActivity)?.let { mainActivity ->
                    mainActivity.loadSite(site.url)
                    mainActivity.saveLastSource(site.id)
                }
                dismiss()
            },
            onSiteDelete = { site ->
                if (site.id.startsWith("custom_")) {
                    showDeleteConfirmDialog(site)
                } else {
                    SiteManager.removeDefaultSite(requireContext(), site.id)
                    loadSites()
                    Toast.makeText(context, "Сайт удалён", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    
    private fun showAddSiteDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_site, null)
        val nameEdit = dialogView.findViewById<TextInputEditText>(R.id.editSiteName)
        val urlEdit = dialogView.findViewById<TextInputEditText>(R.id.editSiteUrl)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Добавить сайт")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val name = nameEdit.text.toString()
                val url = urlEdit.text.toString()
                
                if (SiteManager.addCustomSite(requireContext(), name, url)) {
                    loadSites()
                    Toast.makeText(context, "Сайт добавлен", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Ошибка добавления сайта", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun showDeleteConfirmDialog(site: SiteConfig) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удалить сайт")
            .setMessage("Удалить сайт \"${site.name}\"?")
            .setPositiveButton("Удалить") { _, _ ->
                SiteManager.removeCustomSite(requireContext(), site.id)
                loadSites()
                Toast.makeText(context, "Сайт удален", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}