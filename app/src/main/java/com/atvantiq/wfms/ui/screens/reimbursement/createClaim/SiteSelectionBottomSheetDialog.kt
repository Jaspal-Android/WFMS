package com.atvantiq.wfms.ui.screens.reimbursement.createClaim

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.DialogMultiSelectBottomSheetBinding
import com.atvantiq.wfms.models.site.SiteData
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Locale

class SiteSelectionBottomSheetDialog : BottomSheetDialogFragment() {

    // Data + callback are wired by the host after construction. A no-arg constructor
    // prevents the FragmentManager from throwing InstantiationException on restore, and
    // requireContext() replaces the leaked Activity Context reference.
    var sites: List<SiteData> = emptyList()
    var preSelectedSites: Set<SiteData> = emptySet()
    var onSubmit: ((List<SiteData>) -> Unit)? = null  // returns only selected sites (with selectedPo set)

    private lateinit var binding: DialogMultiSelectBottomSheetBinding
    private lateinit var adapter: SitePoAdapter

    private lateinit var workingSites: MutableList<SiteData>
    private lateinit var selectedSiteIds: MutableSet<Long>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), R.style.AppBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogMultiSelectBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // On process-death-while-open the host callback/data are gone; dismiss
        // gracefully instead of showing a stale, non-functional sheet.
        if (onSubmit == null || sites.isEmpty()) {
            dismiss()
            return
        }

        workingSites = sites.map { site ->
            val preSelectedSite = preSelectedSites.firstOrNull { it.id == site.id }
            site.copy(
                selectedPo = if (preSelectedSite != null) {
                    preSelectedSite.selectedPo ?: if (site.po?.size == 1) site.po[0] else site.selectedPo
                } else null
            )
        }.toMutableList()
        selectedSiteIds = preSelectedSites.map { it.id }.toMutableSet()

        binding.titleTextView.visibility = View.VISIBLE
        binding.titleTextView.text = getString(R.string.select_site)

        adapter = SitePoAdapter(
            sites = workingSites,
            selectedSiteIds = selectedSiteIds,
            onSelectionChanged = { updateSubmitButton() }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        binding.searchBar.addTextChangedListener { text ->
            adapter.filter(text.toString())
        }

        updateSubmitButton()
        binding.submitButton.setOnClickListener {
            val selected = workingSites.filter { it.id in selectedSiteIds }
            onSubmit?.invoke(selected)
            dismiss()
        }
    }

    private fun updateSubmitButton() {
        val selectedSites = workingSites.filter { it.id in selectedSiteIds }
        val allHavePo = selectedSites.isNotEmpty() &&
                selectedSites.all { site ->
                    site.po?.isEmpty() == true || site.selectedPo != null
                }
        binding.submitButton.isEnabled = allHavePo
        binding.submitButton.alpha = if (allHavePo) 1f else 0.5f
    }


    private inner class SitePoAdapter(
        private val sites: MutableList<SiteData>,
        private val selectedSiteIds: MutableSet<Long>,
        private val onSelectionChanged: () -> Unit
    ) : RecyclerView.Adapter<SitePoAdapter.ViewHolder>() {

        private var filteredSites = sites.toMutableList()

        fun filter(query: String) {
            filteredSites = if (query.isEmpty()) {
                sites.toMutableList()
            } else {
                sites.filter {
                    it.name.lowercase(Locale.getDefault())
                        .contains(query.lowercase(Locale.getDefault()))
                }.toMutableList()
            }
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_site_with_po, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(filteredSites[position])
        }

        override fun getItemCount() = filteredSites.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
            private val tvSiteName: TextView = itemView.findViewById(R.id.textView)
            private val layoutPoSection: View = itemView.findViewById(R.id.layoutPoSection)
            private val tvAutoSelectedPo: TextView = itemView.findViewById(R.id.tvAutoSelectedPo)
            private val spinnerPo: Spinner = itemView.findViewById(R.id.spinnerPo)

            fun bind(site: SiteData) {
                val isSelected = site.id in selectedSiteIds
                tvSiteName.text = site.name
                checkBox.setOnCheckedChangeListener(null) // prevent trigger during rebind
                checkBox.isChecked = isSelected

                bindPoSection(site, isSelected)

                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    val currentPosition = adapterPosition
                    if (currentPosition == RecyclerView.NO_POSITION) return@setOnCheckedChangeListener
                    if (isChecked) {
                        selectedSiteIds.add(site.id)
                        // Auto-select PO if only one exists
                        if (site.po?.size == 1) {
                            site.selectedPo = site.po[0]
                        }
                    } else {
                        selectedSiteIds.remove(site.id)
                        site.selectedPo = null
                    }
                    notifyItemChanged(currentPosition)
                    onSelectionChanged()
                }
            }

            private fun bindPoSection(site: SiteData, isSelected: Boolean) {
                if (!isSelected || site.po?.isEmpty() == true) {
                    layoutPoSection.visibility = View.GONE
                    return
                }

                layoutPoSection.visibility = View.VISIBLE

                when (site.po?.size) {
                    1 -> {
                        tvAutoSelectedPo.visibility = View.VISIBLE
                        tvAutoSelectedPo.text = "PO: ${site.po[0].poNumber}"
                        spinnerPo.visibility = View.GONE
                        site.selectedPo = site.po[0] // ensure it's set
                    }
                    else -> {
                        tvAutoSelectedPo.visibility = View.GONE
                        spinnerPo.visibility = View.VISIBLE

                        val poLabels = listOf(itemView.context.getString(R.string.select_po_number)) + (site.po?.map { it.poNumber } ?: emptyList())
                        val spinnerAdapter = ArrayAdapter(
                            itemView.context,
                            android.R.layout.simple_spinner_item,
                            poLabels
                        ).apply {
                            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        }

                        spinnerPo.onItemSelectedListener = null
                        spinnerPo.adapter = spinnerAdapter

                        val restoredIndex = site.po?.indexOfFirst { it.id == site.selectedPo?.id }
                        restoredIndex?.let { spinnerPo.setSelection(if (it >= 0) restoredIndex + 1 else 0) }

                        spinnerPo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: AdapterView<*>, view: View?, pos: Int, id: Long
                            ) {
                                site.selectedPo = if (pos == 0) null else site.po?.get(pos - 1)
                                onSelectionChanged()
                            }
                            override fun onNothingSelected(parent: AdapterView<*>) {}
                        }
                    }
                }
            }
        }
    }
}
