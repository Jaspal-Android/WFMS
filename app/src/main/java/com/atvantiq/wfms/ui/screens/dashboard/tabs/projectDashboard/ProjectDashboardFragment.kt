package com.atvantiq.wfms.ui.screens.dashboard.tabs.projectDashboard

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.databinding.FragmentProjectDashboardBinding
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.adapters.ProjectBudgetAdapter
import com.atvantiq.wfms.utils.DateUtils
import dagger.hilt.android.AndroidEntryPoint

/**
 * Shows a list of project budget cards for the logged-in employee for the selected month.
 */
@AndroidEntryPoint
class ProjectDashboardFragment : BaseFragment<FragmentProjectDashboardBinding, ProjectDashboardVM>() {

    private lateinit var projectAdapter: ProjectBudgetAdapter

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_project_dashboard, ProjectDashboardVM::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {
        // No-op
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        updateMonthChip()
        binding.tvMonthFilter.setOnClickListener { showMonthPicker() }
        binding.ivRefresh.setOnClickListener { viewModel.fetchMyProjects() }
    }

    override fun subscribeToEvents(vm: ProjectDashboardVM) {
        vm.myProjectsResponse.observe(viewLifecycleOwner) { state ->
            when (state.status) {
                Status.LOADING -> showLoading(true)
                Status.SUCCESS -> {
                    showLoading(false)
                    val list = state.response?.data?.items
                    if (!list.isNullOrEmpty()) {
                        binding.rvProjects.visibility = View.VISIBLE
                        binding.tvEmpty.visibility = View.GONE
                        projectAdapter.submitList(list)
                    } else {
                        binding.rvProjects.visibility = View.GONE
                        binding.tvEmpty.text = getString(R.string.no_projects_data)
                        binding.tvEmpty.visibility = View.VISIBLE
                    }
                }
                Status.ERROR -> {
                    showLoading(false)
                    binding.rvProjects.visibility = View.GONE
                    // Distinguish a network/server failure from a genuinely empty month.
                    binding.tvEmpty.text = state.throwable?.message ?: getString(R.string.something_went_wrong)
                    binding.tvEmpty.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupRecyclerView() {
        projectAdapter = ProjectBudgetAdapter()
        binding.rvProjects.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = projectAdapter
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
            viewModel.fetchMyProjects()
        }
    }

    private fun updateMonthChip() {
        binding.tvMonthFilter.text = DateUtils.formatMonthYear(viewModel.selectedMonth, viewModel.selectedYear)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            binding.rvProjects.visibility = View.GONE
            binding.tvEmpty.visibility = View.GONE
        }
    }
}
