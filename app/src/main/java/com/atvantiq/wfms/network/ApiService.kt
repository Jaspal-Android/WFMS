package com.atvantiq.wfms.network
import com.atvantiq.wfms.models.activity.ActivityListByProjectTypeResponse
import com.atvantiq.wfms.models.allProjects.AllProjectsResponse
import com.atvantiq.wfms.models.attendance.CheckInOutResponse
import com.atvantiq.wfms.models.attendance.applyLeave.ApplyLeaveResponse
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceDetailListResponse
import com.atvantiq.wfms.models.attendance.attendanceRemarks.AttendanceRemarksResponse
import com.atvantiq.wfms.models.attendance.checkInStatus.CheckInStatusResponse
import com.atvantiq.wfms.models.circle.CircleListByProjectResponse
import com.atvantiq.wfms.models.client.ClientListResponse
import com.atvantiq.wfms.models.empDetail.EmpDetailResponse
import com.atvantiq.wfms.models.empoyeeByCircle.EmployeeByCircleResponse
import com.atvantiq.wfms.models.forgotPassword.ForgotPasswordResponse
import com.atvantiq.wfms.models.inventory.InventoryByProjectResponse
import com.atvantiq.wfms.models.location.SendLocationResponse
import com.atvantiq.wfms.models.loginResponse.LoginResponse
import com.atvantiq.wfms.models.loginWithOTP.RequestOtpResponse
import com.atvantiq.wfms.models.notification.UpdateNotificationTokenResponse
import com.atvantiq.wfms.models.po.PoListByProjectResponse
import com.atvantiq.wfms.models.project.ProjectListByClientResponse
import com.atvantiq.wfms.models.reimbursement.allClaims.AllClaimsResponse
import com.atvantiq.wfms.models.reimbursement.create.CreateClaimResponse
import com.atvantiq.wfms.models.reimbursement.detail.ClaimDetailResponse
import com.atvantiq.wfms.models.site.SiteListByProjectResponse
import com.atvantiq.wfms.models.site.allSites.SitesListAllResponse
import com.atvantiq.wfms.models.site.create.CreateSiteResponse
import com.atvantiq.wfms.models.type.TypeListByProjectResponse
import com.atvantiq.wfms.models.work.selfAssign.SelfAssignResponse
import com.atvantiq.wfms.models.work.workAssigned.WorkAssignedResponse
import com.atvantiq.wfms.models.work.workDetail.WorkDetailResponse
import com.atvantiq.wfms.models.work.workDetailByDate.WorkDetailsByDateResponse
import com.atvantiq.wfms.models.workSiteByDate.WorkSiteByDateResponse
import com.atvantiq.wfms.models.workSites.approve.ApproveWorkSiteTypeResponse
import com.atvantiq.wfms.models.workSites.workSiteDetails.WorkSiteDetailResponse
import com.atvantiq.wfms.models.workSites.workSites.WorkSitesResponse
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
	/***
	 * Network calls
	 */
	@POST(NetworkEndPoints.loginRequest)
	suspend fun loginRequest(@Body params: JsonObject): LoginResponse

	@GET(NetworkEndPoints.empDetails)
	suspend fun empDetails(@Header("Authorization") token: String): EmpDetailResponse

	@POST(NetworkEndPoints.attendanceCheckIn)
	suspend fun attendanceCheckIn(@Header("Authorization") token: String, @Body params: JsonObject): CheckInOutResponse

	@POST(NetworkEndPoints.attendanceCheckOut)
	suspend fun attendanceCheckOut(@Header("Authorization") token: String,@Body params: JsonObject): CheckInOutResponse

	@GET(NetworkEndPoints.attendanceCheckInStatus)
	suspend fun attendanceCheckInStatus(@Header("Authorization") token: String): CheckInStatusResponse

	@GET(NetworkEndPoints.attendanceDetails)
	suspend fun attendanceDetails(@Header("Authorization") token: String, @Query("month") month: Int,@Query("year") year: Int,@Query("is_export") flag: Boolean ): AttendanceDetailListResponse

	@GET(NetworkEndPoints.workAssignedAll)
	suspend fun workAssignedAll(@Header("Authorization") token: String, @Query("page") page:Int,@Query("page_size") page_size:Int ): WorkAssignedResponse

	@GET(NetworkEndPoints.workSiteDetails)
	suspend fun workById(@Header("Authorization") token: String, @Path("work_site_id") workSiteId:Long): WorkDetailResponse

	@GET(NetworkEndPoints.workDetailByDate)
	suspend fun workDetailByDate(@Header("Authorization") token: String, @Query("date") date: String): WorkDetailsByDateResponse

	@PUT(NetworkEndPoints.workAccept)
	suspend fun workAccept(@Header("Authorization") token: String,@Path("work_site_id") workSiteId:Long): WorkDetailResponse

	@Multipart
	@POST(NetworkEndPoints.workStart)
	suspend fun workStart(
		@Header("Authorization") token: String,
		@Part("work_site_id") workSiteId: RequestBody,
		@Part("latitude") latitude: RequestBody,
		@Part("longitude") longitude: RequestBody,
		@Part photo: MultipartBody.Part
	): WorkDetailResponse

	@POST(NetworkEndPoints.workEnd)
	suspend fun workEnd(@Header("Authorization") token: String, @Body params: JsonObject): WorkDetailResponse

	@POST(NetworkEndPoints.workSelfAssign)
	suspend fun workSelfAssign(@Header("Authorization") token: String, @Body params: JsonObject): SelfAssignResponse

	@GET(NetworkEndPoints.clientList)
	suspend fun clientList(@Header("Authorization") token: String): ClientListResponse

	@GET(NetworkEndPoints.projectListByClientId)
	suspend fun projectListByClientId(@Header("Authorization") token: String, @Path("client_id") clientId: Long, ): ProjectListByClientResponse

	@GET(NetworkEndPoints.poNumberListByProject)
	suspend fun poNumberListByProject(@Header("Authorization") token: String, @Path("project_id") projectId: Long): PoListByProjectResponse

	@GET(NetworkEndPoints.circleByProject)
	suspend fun circleByProject(@Header("Authorization") token: String, @Path("project_id") projectId: Long): CircleListByProjectResponse

	@GET(NetworkEndPoints.siteListByProject)
	suspend fun siteListByProject(@Header("Authorization") token: String, @Path("project_id") projectId: Long): SiteListByProjectResponse

	@GET(NetworkEndPoints.typeListByPo)
	suspend fun typeListByPo(@Header("Authorization") token: String, @Path("po_id") poId: Long): TypeListByProjectResponse

	@GET(NetworkEndPoints.activityListByPoType)
	suspend fun activityListByPoType(
		@Header("Authorization") token: String,
		@Query("po_id") poId: Long,
		@Query("type_id") typeId: Long
	): ActivityListByProjectTypeResponse

	@POST(NetworkEndPoints.geoTrackingLocation)
	suspend fun sendLocation(@Header("Authorization") token: String, @Body params: JsonObject) : SendLocationResponse

	@POST(NetworkEndPoints.notificationToken)
	suspend fun sendNotificationToken(@Header("Authorization") token: String, @Body params: JsonObject) : UpdateNotificationTokenResponse

	@GET(NetworkEndPoints.siteListAll)
	suspend fun siteListAll(@Header("Authorization") token: String, @Query("page") page:Int,@Query("limit") limit:Int,@Query("is_active") is_active:Int): SitesListAllResponse

	@POST(NetworkEndPoints.createSite)
	suspend fun createSite(@Header("Authorization") token: String, @Body params: JsonObject): CreateSiteResponse

	@GET(NetworkEndPoints.workSites)
	suspend fun workSites(@Header("Authorization") token: String, @Path("employee_id") employeeId: String, @Query("date") date: String): WorkSitesResponse

	@GET(NetworkEndPoints.workSiteDetailsAdmin)
	suspend fun workSiteDetailsAdmin(@Header("Authorization") token: String, @Path("work_site_id") workSiteId: Long, @Query("employee_id") employeeId: String, @Query("date") date: String): WorkSiteDetailResponse

	@POST(NetworkEndPoints.approveWorkSite)
	suspend fun approveWorkSite(@Header("Authorization") token: String, @Body params: JsonArray): ApproveWorkSiteTypeResponse

	@POST(NetworkEndPoints.attendanceEmpRemarks)
	suspend fun attendanceEmpRemarks(@Header("Authorization") token: String, @Path("attendance_id") attendanceId: Long,@Body params: JsonObject) : AttendanceRemarksResponse

	@POST(NetworkEndPoints.forgotPassword)
	suspend fun forgotPassword(@Header("Authorization") token: String, @Body params: JsonObject): ForgotPasswordResponse

	@Multipart
	@POST(NetworkEndPoints.applyLeave)
	suspend fun applyLeave(
		@Header("Authorization") token: String,
		@Part("leave_type") leaveType: RequestBody,
		@Part("from_date") fromDate: RequestBody,
		@Part("to_date") toDate: RequestBody,
		@Part("reason") reason: RequestBody,
		@Part attachment: MultipartBody.Part?
	): ApplyLeaveResponse

	@POST(NetworkEndPoints.requestOTP)
	suspend fun requestOTP(@Body params: JsonObject): RequestOtpResponse

	@POST(NetworkEndPoints.verifyOTP)
	suspend fun verifyOTP(@Body params: JsonObject): LoginResponse

	@GET(NetworkEndPoints.workSiteByDate)
	suspend fun workSiteByDate(@Header("Authorization") token: String, @Query("date") date: String): WorkSiteByDateResponse

	@GET(NetworkEndPoints.allProjects)
	suspend fun allProjects(@Header("Authorization") token: String): AllProjectsResponse

	@GET(NetworkEndPoints.circleEmployees)
	suspend fun employeeByCircle(@Header("Authorization") token: String, @Query("code") circleId: String): EmployeeByCircleResponse

	@Multipart
	@POST(NetworkEndPoints.createClaim)
	suspend fun createClaim(
		@Header("Authorization") token: String,
		@Part("data") data: RequestBody,
		@Part files: List<MultipartBody.Part>
	): CreateClaimResponse

	@GET(NetworkEndPoints.allClaims)
	suspend fun allClaims(@Header("Authorization") token: String, @Query("page") page:Int,@Query("page_size") pageSize:Int ): AllClaimsResponse

	@GET(NetworkEndPoints.claimById)
	suspend fun claimById(@Header("Authorization") token: String, @Path("claim_id") claimId: Long): ClaimDetailResponse

    @GET(NetworkEndPoints.inventoryByProject)
    suspend fun inventoryByProject(@Header("Authorization") token: String, @Path("project_id") projectId: Long): InventoryByProjectResponse

}