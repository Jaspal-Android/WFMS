package com.atvantiq.wfms.network

object NetworkEndPoints {
	
	/* Login Page */
	//const val  BASE_URL = "http://69.62.85.16:8000/"
	//const val  BASE_URL = "http://69.62.85.16:8000/"
	const val  BASE_URL = "http://69.62.85.16:8000/"

	/*Login*/
	const val loginRequest = "login"
	const val empDetails = "employee/me"

	/*Day Attendance*/
	const val attendanceCheckIn = "attendance/checkin"
	const val attendanceCheckOut = "attendance/checkout"
	const val attendanceCheckInStatus = "attendance/checkin/status"
	const val attendanceDetails = "attendance/details"

	/*Attendance Management*/
	const val workAssignedAll = "work/all"
	const val workAccept = "work/accept/{work_id}"
	const val workStart = "work/start"
	const val workEnd = "work/end"
	const val workSelfAssign = "work/self-assign"
	const val workById = "work/{work_id}"
	const val workDetailByDate = "work/details"

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

	/*Type*/
	const val typeListByPorject = "type/project/{project_id}"

	/*Activity*/
	const val activityListByPorjectType = "activity/project-type"
}