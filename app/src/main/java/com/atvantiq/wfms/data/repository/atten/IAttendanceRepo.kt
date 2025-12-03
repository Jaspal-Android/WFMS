package com.atvantiq.wfms.data.repository.atten

import com.atvantiq.wfms.models.attendance.CheckInOutResponse
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceDetailListResponse
import com.atvantiq.wfms.models.attendance.attendanceRemarks.AttendanceRemarksResponse
import com.atvantiq.wfms.models.attendance.checkInStatus.CheckInStatusResponse
import com.atvantiq.wfms.models.workSites.WorkSitesResponse
import com.atvantiq.wfms.models.workSites.approve.ApproveWorkSiteResponse
import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface IAttendanceRepo {

    suspend fun attendanceCheckInRequest(params: JsonObject): CheckInOutResponse

    suspend fun attendanceCheckOutRequest(params: JsonObject): CheckInOutResponse

    suspend fun attendanceCheckInStatus(): CheckInStatusResponse

    suspend fun attendanceDetails(month: Int, year: Int): AttendanceDetailListResponse

    suspend fun workSites(employeeId: String,date: String): WorkSitesResponse

    suspend fun approveWorkSite(params: JsonObject): ApproveWorkSiteResponse

    suspend fun attendanceEmpRemarks(attendanceId: Long,params: JsonObject) : AttendanceRemarksResponse

}