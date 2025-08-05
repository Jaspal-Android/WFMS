package com.atvantiq.wfms.network
import com.atvantiq.wfms.models.activity.ActivityListByProjectTypeResponse
import com.atvantiq.wfms.models.attendance.CheckInOutResponse
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceDetailListResponse
import com.atvantiq.wfms.models.attendance.checkInStatus.CheckInStatusResponse
import com.atvantiq.wfms.models.circle.CircleListByProjectResponse
import com.atvantiq.wfms.models.client.ClientListResponse
import com.atvantiq.wfms.models.loginResponse.LoginResponse
import com.atvantiq.wfms.models.po.PoListByProjectResponse
import com.atvantiq.wfms.models.project.ProjectListByClientResponse
import com.atvantiq.wfms.models.site.SiteListByProjectResponse
import com.atvantiq.wfms.models.type.TypeListByProjectResponse
import com.atvantiq.wfms.models.work.acceptWork.AcceptWorkResponse
import com.atvantiq.wfms.models.work.assignedAll.WorkAssignedAllResponse
import com.atvantiq.wfms.models.work.endWork.EndWorkResponse
import com.atvantiq.wfms.models.work.selfAssign.SelfAssignResponse
import com.atvantiq.wfms.models.work.startWork.StartWorkResponse
import com.atvantiq.wfms.models.work.workDetail.WorkDetailResponse
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

	@POST(NetworkEndPoints.attendanceCheckIn)
	suspend fun attendanceCheckIn(@Header("Authorization") token: String, @Body params: JsonObject): CheckInOutResponse

	@POST(NetworkEndPoints.attendanceCheckOut)
	suspend fun attendanceCheckOut(@Header("Authorization") token: String,@Body params: JsonObject): CheckInOutResponse

	@GET(NetworkEndPoints.attendanceCheckInStatus)
	suspend fun attendanceCheckInStatus(@Header("Authorization") token: String): CheckInStatusResponse

	@GET(NetworkEndPoints.attendanceDetails)
	suspend fun attendanceDetails(@Header("Authorization") token: String, @Query("month") month: Int,@Query("year") year: Int): AttendanceDetailListResponse

	@GET(NetworkEndPoints.workAssignedAll)
	suspend fun workAssignedAll(@Header("Authorization") token: String, @Query("page") page:Int,@Query("page_size") page_size:Int ): WorkAssignedAllResponse

	@GET(NetworkEndPoints.workById)
	suspend fun workById(@Header("Authorization") token: String, @Path("work_id") workId:Int): WorkDetailResponse

	@PUT(NetworkEndPoints.workAccept)
	suspend fun workAccept(@Header("Authorization") token: String,@Path("work_id") workId:Int): AcceptWorkResponse

	@Multipart
	@POST(NetworkEndPoints.workStart)
	suspend fun workStart(
		@Header("Authorization") token: String,
		@Part("work_id") workId: RequestBody,
		@Part("latitude") latitude: RequestBody,
		@Part("longitude") longitude: RequestBody,
		@Part photo: MultipartBody.Part
	): StartWorkResponse

	@POST(NetworkEndPoints.workEnd)
	suspend fun workEnd(@Header("Authorization") token: String, @Body params: JsonObject): EndWorkResponse

	@POST(NetworkEndPoints.workSelfAssign)
	suspend fun workSelfAssign(@Header("Authorization") token: String, @Body params: JsonObject): SelfAssignResponse

	@GET(NetworkEndPoints.clientList)
	suspend fun clientList(@Header("Authorization") token: String): ClientListResponse

	@GET(NetworkEndPoints.projectListByClientId)
	suspend fun projectListByClientId(@Header("Authorization") token: String, @Path("client_id") clientId: Int, ): ProjectListByClientResponse

	@GET(NetworkEndPoints.poNumberListByProject)
	suspend fun poNumberListByProject(@Header("Authorization") token: String, @Path("project_id") projectId: Int): PoListByProjectResponse

	@GET(NetworkEndPoints.circleByProject)
	suspend fun circleByProject(@Header("Authorization") token: String, @Path("project_id") projectId: Int): CircleListByProjectResponse

	@GET(NetworkEndPoints.siteListByProject)
	suspend fun siteListByProject(@Header("Authorization") token: String, @Path("project_id") projectId: Int): SiteListByProjectResponse

	@GET(NetworkEndPoints.typeListByPorject)
	suspend fun typeListByProject(@Header("Authorization") token: String, @Path("project_id") projectId: Int): TypeListByProjectResponse

	@GET(NetworkEndPoints.activityListByPorjectType)
	suspend fun activityListByProjectType(
		@Header("Authorization") token: String,
		@Query("project_id") projectId: Int,
		@Query("type_id") typeId: Int
	): ActivityListByProjectTypeResponse
}