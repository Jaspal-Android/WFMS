package com.atvantiq.wfms.ui.screens.dashboard.tabs.myTargets

import android.os.Bundle
import android.view.View
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.databinding.FragmentMyTargetsBinding
import com.atvantiq.wfms.models.targets.MyTargetsData
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.utils.DateUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MyTargetsFragment : BaseFragment<FragmentMyTargetsBinding, MyTargetsVM>() {

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_my_targets, MyTargetsVM::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {
        // No-op
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateMonthChip()
        binding.tvMonthFilter.setOnClickListener { showMonthPicker() }
        binding.ivRefresh.setOnClickListener { viewModel.fetchMyTargets() }
    }

    override fun subscribeToEvents(vm: MyTargetsVM) {
        vm.myTargetsResponse.observe(viewLifecycleOwner) { state ->
            when (state.status) {
                Status.LOADING -> showLoading(true)
                Status.SUCCESS -> {
                    showLoading(false)
                    val data = state.response?.data
                    if (data != null) {
                        showContent(true)
                        bindData(data)
                    } else {
                        showContent(false)
                        binding.tvEmpty.visibility = View.VISIBLE
                    }
                }
                Status.ERROR -> {
                    showLoading(false)
                    showContent(false)
                    binding.tvEmpty.visibility = View.VISIBLE
                    showToast(requireContext(), state.throwable?.message ?: getString(R.string.please_wait))
                }
            }
        }
    }

    private fun showMonthPicker() {
        DateUtils.showMonthYearPickerDialog(
            context = requireContext(),
            month = viewModel.selectedMonth,
            year = viewModel.selectedYear
        ) { month, year ->
            viewModel.selectedMonth = month
            viewModel.selectedYear = year
            updateMonthChip()
            viewModel.fetchMyTargets()
        }
    }

    private fun updateMonthChip() {
        binding.tvMonthFilter.text = DateUtils.formatMonthYear(viewModel.selectedMonth, viewModel.selectedYear)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            binding.tvEmpty.visibility = View.GONE
            showContent(false)
        }
    }

    private fun showContent(visible: Boolean) {
        binding.layoutContent.visibility = if (visible) View.VISIBLE else View.GONE
        if (!visible) binding.tvEmpty.visibility = View.GONE
    }

    private fun bindData(data: MyTargetsData) {
        bindPeriodCard(data)
        bindRevenueCard(data)
        bindStatsCard(data)
    }

    private fun bindPeriodCard(data: MyTargetsData) {
        binding.tvPeriod.text = DateUtils.formatYearMonthString(data.month)

        val isOnTrack = data.status?.lowercase() == "on_track"
        binding.tvStatus.apply {
            text = if (isOnTrack) getString(R.string.on_track) else getString(R.string.behind)
            setTextColor(
                resources.getColor(
                    if (isOnTrack) R.color.status_present_text else R.color.red,
                    requireContext().theme
                )
            )
            setBackgroundResource(
                if (isOnTrack) R.drawable.bg_status_on_track else R.drawable.bg_status_behind
            )
        }
    }

    private fun bindRevenueCard(data: MyTargetsData) {
        val revenue = data.revenue ?: return

        binding.tvRevenueAchieved.text = formatCurrency(revenue.achieved)
        binding.tvRevenueTarget.text = "of ${formatCurrency(revenue.target)} target"

        val variation = revenue.variation ?: 0.0
        binding.tvRevenueDifference.apply {
            text = formatCurrency(Math.abs(variation))
            setTextColor(
                resources.getColor(
                    if (variation > 0) R.color.red else R.color.green,
                    requireContext().theme
                )
            )
        }

        // achievementPercentage comes as "0.0%" or "54.55%" — strip % and parse
        val pct = stripPct(revenue.achievementPercentage).toInt().coerceIn(0, 100)
        binding.progressRevenue.progress = pct
        binding.tvRevenueAchievedPct.text = "$pct% ${getString(R.string.achieved)}"
        binding.tvRevenueRemainingPct.text = "${100 - pct}% ${getString(R.string.to_go)}"
    }

    private fun bindStatsCard(data: MyTargetsData) {
        val sites = data.sites
        if (sites != null) {
            binding.tvSitesValue.text = "${sites.achieved ?: 0} / ${sites.target ?: 0}"
            binding.tvSitesPct.text = formatPctString(sites.achievementPercentage)
        }

        val days = data.activeDays
        if (days != null) {
            binding.tvActiveDaysValue.text = "${days.achieved ?: 0} / ${days.target ?: 0}"
            binding.tvActiveDaysPct.text = formatPctString(days.achievementPercentage)
        }

        val hours = data.hoursWorked
        if (hours != null) {
            val achieved = hours.achieved?.toInt() ?: 0
            val target = hours.target?.toInt() ?: 0
            binding.tvHoursWorkedValue.text = "$achieved / $target"
            binding.tvHoursWorkedPct.text = formatPctString(hours.achievementPercentage)
        }

        // efficiency is already a formatted String from the API e.g. "60.07%"
        binding.tvEfficiencyValue.text = data.efficiency ?: "-"
    }

    /** Strips trailing % and parses to Double. Returns 0.0 on failure. */
    private fun stripPct(value: String?): Double =
        value?.trimEnd('%')?.toDoubleOrNull() ?: 0.0

    /** Displays an API percentage string as-is, with fallback. */
    private fun formatPctString(value: String?): String =
        if (value.isNullOrBlank()) "-" else value

    private fun formatCurrency(amount: Double?): String {
        if (amount == null) return "-"
        val abs = Math.abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            abs >= 1_000_000 -> "${sign}${"$"}${String.format("%.1f", abs / 1_000_000)}M"
            abs >= 1_000 -> "${sign}${"$"}${String.format("%.0f", abs / 1_000)}K"
            else -> "${sign}${"$"}${String.format("%.0f", abs)}"
        }
    }
}
