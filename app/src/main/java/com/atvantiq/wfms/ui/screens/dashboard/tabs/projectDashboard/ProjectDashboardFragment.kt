package com.atvantiq.wfms.ui.screens.dashboard.tabs.projectDashboard

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseBindingFragment
import com.atvantiq.wfms.databinding.FragmentProjectDashboardBinding
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate

/**
 * A simple [Fragment] subclass.
 * Use the [ProjectDashboardFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

class ProjectDashboardFragment : BaseBindingFragment<FragmentProjectDashboardBinding>() {

    private val projectEntries = ArrayList<BarEntry>()
    private val labels = ArrayList<String>()

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_project_dashboard)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {

    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        // Sample data for projects
        projectEntries.add(BarEntry(0f, 45f)) // Project 1: 80%
        projectEntries.add(BarEntry(1f, 80f)) // Project 2: 70%
        projectEntries.add(BarEntry(2f, 60f)) // Project 3: 90%

        labels.add("Cisco")
        labels.add("Jio Towers")
        labels.add("Airtel Netwok")

        // Create a BarDataSet
        val barDataSet = BarDataSet(projectEntries, "Atvantiq Networks").apply {
            colors = listOf(ActivityCompat.getColor(requireContext(),R.color.green))
            valueTextSize = 14f
            valueTextColor = Color.BLACK
        }

        // Add the dataset to BarData
        val barData = BarData(barDataSet)

        // Configure XAxis
        val xAxis = binding.barChart.xAxis.apply {
            isGranularityEnabled = false
            valueFormatter = IndexAxisValueFormatter(labels)
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
        }

        // Configure YAxis
        binding.barChart.axisLeft.apply {
            axisMaximum = 100f // Set max value
            granularity = 10f
        }

        binding.barChart.axisRight.isEnabled = false

        // Configure the chart
        binding.barChart.apply {
            description = Description().apply { text = "Progress Graph" }
            data = barData
            animateY(1000)
            invalidate() // Refresh the chart
        }

    }

}