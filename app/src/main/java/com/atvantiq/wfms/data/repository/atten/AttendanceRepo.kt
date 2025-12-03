package com.atvantiq.wfms.data.repository.atten

import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceDetailListResponse
import com.atvantiq.wfms.models.attendance.attendanceRemarks.AttendanceRemarksResponse
import com.atvantiq.wfms.models.attendance.checkInStatus.CheckInStatusResponse
import com.atvantiq.wfms.models.workSites.WorkSitesResponse
import com.atvantiq.wfms.models.workSites.approve.ApproveWorkSiteResponse
import com.atvantiq.wfms.network.ApiService
import com.google.gson.JsonObject
import com.ssas.jibli.data.prefs.PrefKeys
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AttendanceRepo @Inject constructor(
    private val apiService: ApiService,
    private val prefMain: SecurePrefMain
) : IAttendanceRepo {

    override suspend fun attendanceCheckInRequest(params: JsonObject) =
        apiService.attendanceCheckIn("Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN, ""), params)

    override suspend fun attendanceCheckOutRequest(params: JsonObject) =
        apiService.attendanceCheckOut("Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN, ""), params)

    override suspend fun attendanceCheckInStatus(): CheckInStatusResponse {
        return apiService.attendanceCheckInStatus(
            "Bearer " + prefMain.get(
                PrefKeys.LOGIN_TOKEN,
                ""
            )
        )
    }

    override suspend fun attendanceDetails(month: Int, year: Int): AttendanceDetailListResponse {
        return apiService.attendanceDetails(
            "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN, ""),
            month,
            year
        )
    }

    override suspend fun workSites(employeeId: String, date: String): WorkSitesResponse =
        apiService.workSites(
            "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN, ""),
            employeeId,
            date
        )

    override suspend fun approveWorkSite(
        params: JsonObject
    ): ApproveWorkSiteResponse = apiService.approveWorkSite(
        "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN, ""),
        params
    )

    override suspend fun attendanceEmpRemarks(attendanceId: Long,params: JsonObject): AttendanceRemarksResponse  = apiService.attendanceEmpRemarks(
        "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN, ""),
        attendanceId,
        params
    )
}