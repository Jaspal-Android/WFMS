package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.databinding.ItemAttendanceListBinding
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceRecord
import com.atvantiq.wfms.utils.DateUtils

class AttendanceListAdapter(var onViewSitesClicked:(employeeId: String,date: String)->Unit) : RecyclerView.Adapter<AttendanceListAdapter.Holder>(){

    private var attendanceRecordList =  ArrayList<AttendanceRecord>()

    inner class Holder(var binding:ItemAttendanceListBinding):
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        var infalter = LayoutInflater.from(parent.context)
        var binding:ItemAttendanceListBinding = ItemAttendanceListBinding.inflate(infalter, parent, false)
        return Holder(binding)
    }

    override fun getItemCount(): Int {
        return attendanceRecordList.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        var record = attendanceRecordList[position]
        holder.binding.item = record
        holder.binding.viewSitesBt.setOnClickListener {
            var date = DateUtils.formatApiDateToYMD(record.createdAt?:"")
            onViewSitesClicked.invoke((record?.employee?.id).toString(),date?:"")
        }
        holder.binding.executePendingBindings()
    }

    fun submitData(data: List<AttendanceRecord>){
        attendanceRecordList.clear()
        attendanceRecordList.addAll(data)
        notifyDataSetChanged()
    }
}