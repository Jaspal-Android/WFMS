package com.ssas.jibli.data.prefs

import androidx.security.crypto.EncryptedSharedPreferences
import com.atvantiq.wfms.data.prefs.PrefMain
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.models.empDetail.EmpData
import com.atvantiq.wfms.models.loginResponse.User
import com.google.gson.Gson


object PrefMethods {

	/*
	* Saving user token for session management
	* */
	fun saveUserToken(pref: SecurePrefMain, token:String?){
		pref.put(PrefKeys.LOGIN_TOKEN,token)
	}

	fun getUserToken(pref: SecurePrefMain):String?= pref.get(PrefKeys.LOGIN_TOKEN,null)

	/***
	 * Saving profile detail data
	 */
	fun saveUserData(
		prefMain: SecurePrefMain,
		userData: User?
	) {
		if (userData != null) {
			val serialProfileData = Gson().toJson(userData)
			prefMain.put(PrefKeys.USER_DATA, serialProfileData)
		}
	}

	fun getUserData(prefMain: SecurePrefMain) : User? {
		var userDataString = prefMain[PrefKeys.USER_DATA, ""]
		var userDataModel = Gson().fromJson<User>(userDataString, User::class.java)
		return userDataModel
	}

	/**
	 * Save EmpDetailResponse data
	 */
	fun saveEmpDetailResponse(
		prefMain: SecurePrefMain,
		empDetailResponse: EmpData?
	) {
		if (empDetailResponse != null) {
			val serializedData = Gson().toJson(empDetailResponse)
			prefMain.put(PrefKeys.EMP_DATA, serializedData)
		}
	}

	/**
	 * Retrieve EmpDetailResponse data
	 */
	fun getEmpDetailResponse(prefMain: SecurePrefMain): EmpData? {
		val empDetailString = prefMain[PrefKeys.EMP_DATA, ""]
		return Gson().fromJson(empDetailString, EmpData::class.java)
	}

}