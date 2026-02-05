package com.atvantiq.wfms.network

object NetworkEndPoints {

	/*Login*/
	const val loginRequest = "login"
	const val empDetails = "employee/me"

	/*Forgot password*/
	const val forgotPassword = "forgot-password"
	const val requestOTP = "request-otp"
	const val verifyOTP="verify-otp"

	/*Day Attendance*/
	const val attendanceCheckIn = "attendance/checkin"
	const val attendanceCheckOut = "attendance/checkout"
	const val attendanceCheckInStatus = "attendance/checkin/status"
	const val attendanceDetails = "attendance/details"

	/*Work Management*/
	const val workAssignedAll = "work/site/assigned"
	const val workAccept = "work/accept/{work_site_id}"
	const val workStart = "work/start"
	const val workEnd = "work/end"
	const val workSelfAssign = "work/self-assign"
	const val workById = "work/{work_id}"
	const val workSiteDetails ="work/site/{work_site_id}/types"
	const val workDetailByDate = "work/details"
	const val attendanceEmpRemarks = "attendance/emp/remarks/{attendance_id}"
	const val applyLeave  = "attendance/apply-leave"

	/*Client*/
	const val clientList = "client/all"

	/*Project*/
	const val projectListByClientId = "project/client/{client_id}"

	/*PO Number*/
	const val poNumberListByProject = "po/rec/{project_id}"

	/*Circle*/
	const val circleByProject = "project/circle/{project_id}"

	/*Site*/
	const val siteListByProject = "site/project/{project_id}"
	const val createSite="site/create"
	const val workSites = "work/sites/{employee_id}"
	const val workSiteDetailsAdmin ="/work/site/progress/{work_site_id}"
	const val approveWorkSite = "work/approve"

	/*Sites all by page and limit*/
	const val siteListAll = "site/all"

	/*Type*/
	//const val typeListByPorject = "type/project/{project_id}"
	const val typeListByPo = "type/po/{po_id}"
	/*Activity*/
	//const val activityListByPorjectType = "activity/project-type"
	const val activityListByPoType = "activity/po-type"

	/*Send Geo Location*/
	const val geoTrackingLocation="geo-tracking/location"

	/*Notification*/
	const val notificationToken="notifications/notification-token"

	/*Claims*/
	const val workSiteByDate = "claim/sites/date"

	/*All Projects*/
	const val allProjects = "project/all?is_all=true&is_active=1"

	/*Get circle employes*/
	const val circleEmployees = "circle/employee"

	/*Create claim*/
	const val createClaim = "claim/create-claim"

	/*All claims*/
	const val allClaims = "claim/all/mobile"

	/*Claim by ID*/
	const val claimById = "claim/{claim_id}"
}