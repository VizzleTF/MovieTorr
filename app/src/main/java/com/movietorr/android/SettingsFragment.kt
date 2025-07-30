package com.movietorr.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.movietorr.android.databinding.FragmentSettingsBinding

class SettingsFragment : DialogFragment() {
    
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var allSitesAdapter: AllSitesAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAllSitesRecycler()
        setupAddSiteButton()
        setupKeyboardHandling()
        setupThemeSelection()
        binding.btnLegalInfo?.setOnClickListener {
            showLegalInfoDialog()
        }
    }

    override fun onStart() {
        super.onStart()
        // Центрируем диалог программно
        dialog?.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            // Центрируем диалог
            setGravity(android.view.Gravity.CENTER)
            
            // Блюр для Android 12+ (как backdrop-filter в CSS)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                setBackgroundBlurRadius(16)
            }
        }
    }
    
    private fun setupAllSitesRecycler() {
        allSitesAdapter = AllSitesAdapter { siteId ->
            // Удаляем сайт (может быть как дефолтный, так и пользовательский)
            if (siteId.startsWith("custom_")) {
                SiteSettingsManager.removeCustomSite(requireContext(), siteId)
            } else {
                SiteSettingsManager.removeDefaultSite(requireContext(), siteId)
            }
            updateAllSitesList()
        }
        
        binding.recyclerAllSites.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = allSitesAdapter
        }
        
        updateAllSitesList()
    }
    
    private fun setupAddSiteButton() {
        binding.btnAddSite.setOnClickListener {
            val name = binding.editSiteName.text.toString().trim()
            val url = binding.editSiteUrl.text.toString().trim()
            
            if (name.isBlank() || url.isBlank()) {
                Toast.makeText(context, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                Toast.makeText(context, "URL должен начинаться с http:// или https://", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (SiteSettingsManager.addCustomSite(requireContext(), name, url)) {
                binding.editSiteName.text?.clear()
                binding.editSiteUrl.text?.clear()
                updateAllSitesList()
                Toast.makeText(context, "Сайт добавлен", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Ошибка при добавлении сайта", Toast.LENGTH_SHORT).show()
            }
        }
    }
    

    
    private fun updateAllSitesList() {
        val allSites = SiteSettingsManager.getAllSites(requireContext())
        allSitesAdapter.updateSites(allSites)
    }
    
    private fun setupKeyboardHandling() {
        // Настраиваем обработку фокуса для полей ввода
        binding.editSiteName.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Прокручиваем к полю при получении фокуса
                binding.root.post {
                    binding.root.requestLayout()
                }
            }
        }
        
        binding.editSiteUrl.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Прокручиваем к полю при получении фокуса
                binding.root.post {
                    binding.root.requestLayout()
                }
            }
        }
    }

    private fun setupThemeSelection() {
        val sharedPrefs = requireContext().getSharedPreferences("MovieTorrPrefs", android.content.Context.MODE_PRIVATE)
        val currentTheme = sharedPrefs.getInt("theme_mode", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        
        // Устанавливаем текущую выбранную тему
        when (currentTheme) {
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO -> binding.themeLight.isChecked = true
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES -> binding.themeDark.isChecked = true
            else -> binding.themeAuto.isChecked = true
        }
        
        // Настраиваем обработчики
        binding.themeAuto.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                sharedPrefs.edit().putInt("theme_mode", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM).apply()
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
        
        binding.themeLight.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                sharedPrefs.edit().putInt("theme_mode", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO).apply()
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
        
        binding.themeDark.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                sharedPrefs.edit().putInt("theme_mode", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES).apply()
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }
    
    private fun showLegalInfoDialog() {
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.legal_notice_title)
            .setMessage(android.text.Html.fromHtml(getString(R.string.legal_notice_content), android.text.Html.FROM_HTML_MODE_COMPACT))
            .setPositiveButton(R.string.ok) { d, _ -> d.dismiss() }
            .setCancelable(true)
            .create()
        dialog.show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class AllSitesAdapter(
    private val onDeleteClick: (String) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<AllSitesAdapter.ViewHolder>() {
    
    private var sites: List<SiteConfig> = emptyList()
    
    fun updateSites(newSites: List<SiteConfig>) {
        sites = newSites
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_custom_site, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(sites[position])
    }
    
    override fun getItemCount() = sites.size
    
    inner class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        private val tvSiteName: android.widget.TextView = itemView.findViewById(R.id.tv_site_name)
        private val tvSiteUrl: android.widget.TextView = itemView.findViewById(R.id.tv_site_url)
        private val btnDelete: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btn_delete_site)
        
        fun bind(site: SiteConfig) {
            tvSiteName.text = site.name
            tvSiteUrl.text = site.url
            btnDelete.setOnClickListener { onDeleteClick(site.id) }
        }
    }
}

 