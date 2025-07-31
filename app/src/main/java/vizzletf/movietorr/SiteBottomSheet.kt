package vizzletf.movietorr

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
        
        view.findViewById<MaterialButton>(R.id.btnQuickSite).setOnClickListener {
            showQuickSiteDialog()
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
                    Toast.makeText(context, getString(R.string.site_removed), Toast.LENGTH_SHORT).show()
                }
            },
            onSiteEdit = { site ->
                if (site.id.startsWith("custom_")) {
                    showEditSiteDialog(site)
                } else {
                    Toast.makeText(context, getString(R.string.site_builtin_edit_error), Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    
    private fun showAddSiteDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_site, null)
        val nameEdit = dialogView.findViewById<TextInputEditText>(R.id.editSiteName)
        val urlEdit = dialogView.findViewById<TextInputEditText>(R.id.editSiteUrl)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_edit_site))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.dialog_add)) { _, _ ->
                val name = nameEdit.text.toString()
                val url = urlEdit.text.toString()
                
                if (SiteManager.addCustomSite(requireContext(), name, url)) {
                    loadSites()
                    Toast.makeText(context, getString(R.string.site_added), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, getString(R.string.site_add_error), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }
    
    private fun showQuickSiteDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_quick_site, null)
        val urlEdit = dialogView.findViewById<TextInputEditText>(R.id.editSiteUrl)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_quick_site))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.dialog_go)) { _, _ ->
                val url = urlEdit.text.toString()
                if (url.isNotBlank()) {
                    (activity as? MainActivity)?.let { mainActivity ->
                        mainActivity.loadSite(url)
                    }
                    dismiss()
                } else {
                    Toast.makeText(context, getString(R.string.site_enter_address), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }
    
    private fun showEditSiteDialog(site: SiteConfig) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_site, null)
        val nameEdit = dialogView.findViewById<TextInputEditText>(R.id.editSiteName)
        val urlEdit = dialogView.findViewById<TextInputEditText>(R.id.editSiteUrl)
        
        // Заполняем поля текущими значениями
        nameEdit.setText(site.name)
        urlEdit.setText(site.url)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_edit_site))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.dialog_save)) { _, _ ->
                val name = nameEdit.text.toString()
                val url = urlEdit.text.toString()
                
                if (SiteManager.updateCustomSite(requireContext(), site.id, name, url)) {
                    loadSites()
                    Toast.makeText(context, getString(R.string.site_updated), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, getString(R.string.site_edit_error), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }
    
    private fun showDeleteConfirmDialog(site: SiteConfig) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_delete_site))
            .setMessage(getString(R.string.site_delete_confirm, site.name))
            .setPositiveButton(getString(R.string.dialog_delete)) { _, _ ->
                SiteManager.removeCustomSite(requireContext(), site.id)
                loadSites()
                Toast.makeText(context, getString(R.string.site_deleted), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }
}