package com.atvantiq.wfms.ui.screens.login

import android.app.Application
import com.atvantiq.wfms.base.LiveEvent
import timber.log.Timber
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.data.repository.auth.IAuthRepo
import com.atvantiq.wfms.models.loginResponse.LoginResponse
import com.atvantiq.wfms.models.loginResponse.User
import com.atvantiq.wfms.models.loginWithOTP.RequestOtpResponse
import com.atvantiq.wfms.network.ApiState
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.JsonObject
import com.ssas.jibli.data.prefs.PrefMethods
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@HiltViewModel
class LoginVM @Inject constructor(
    application: Application,
    private val authRepo: IAuthRepo,
    private val prefMain: SecurePrefMain
) : BaseViewModel(application) {

    var isPasswordVisible = true
    val userName = MutableLiveData<String>("")
    val password = MutableLiveData<String>("")
    val isButtonEnabled = MutableLiveData<Boolean>(true)
    var user:User?=null
    val userEmailId = ObservableField<String>().apply { set("") }

    val clickEvents = LiveEvent<LoginClickEvents>()
    val errorHandler = LiveEvent<LoginErrorHandler>()
    val loginResponse = MutableLiveData<ApiState<LoginResponse>>()

    /**
     * True once an accepted login is saved and both the profile fetch and push-token registration
     * have finished, failed or timed out. LiveData only delivers it while the screen is visible, so a login that completes
     * in the background opens the dashboard when the user comes back.
     */
    val loginCompleted = LiveEvent<Boolean>()
    private var isCompletingLogin = false

    /** Fetches this device's push token; replaced in tests. */
    internal var fetchPushToken: suspend () -> String = { firebasePushToken() }
    val requestOtpResponse = MutableLiveData<ApiState<RequestOtpResponse>>()

    // Click event handlers
    fun onForgetPasswordClick() = postClickEvent(LoginClickEvents.ON_FORGET_PASSWORD_CLICK)
    fun onSubmitLoginClick() { if (isValidLoginDetails()) loginRequest() }
    fun onLoginWithOtpClick() = postClickEvent(LoginClickEvents.ON_LOGIN_WITH_OTP_CLICK)
    fun onPasswordToggleClick() = postClickEvent(LoginClickEvents.ON_PASSWORD_TOGGLE)
    fun onFetchCurrentLatitudeLongitudeClicks() = postClickEvent(LoginClickEvents.ON_FETCH_CURRENT_LATITUDE_LONGITUDE_CLICKS)

    private fun postClickEvent(event: LoginClickEvents) {
        clickEvents.value = event
    }

    private fun isValidLoginDetails(): Boolean {
        return when {
            userName.value.isNullOrBlank() -> {
                errorHandler.value = LoginErrorHandler.EMPTY_USERNAME
                false
            }
            password.value.isNullOrBlank() -> {
                errorHandler.value = LoginErrorHandler.EMPTY_PASSWORD
                false
            }
            else -> true
        }
    }

    private fun loginRequest() {
        val params = JsonObject().apply {
            addProperty("email", userName.value.orEmpty().trim())
            addProperty("password", password.value.orEmpty())
        }
        isButtonEnabled.value = false
        executeApiCall(
            apiCall = { authRepo.loginRequest(params) },
            liveData = loginResponse,
            onSuccess = { response ->
                isButtonEnabled.value = true
                onLoginAnswered(response)
            },
            onError = { isButtonEnabled.value = true }
        )
    }

    fun isAccepted(response: LoginResponse?): Boolean =
        response != null && response.code == ValConstants.SUCCESS_CODE && response.success

    /**
     * Saves an accepted session, then (both best-effort, side by side) caches the profile, whose
     * role and permissions pick the dashboard tabs, and registers the push token; then completes.
     */
    private fun onLoginAnswered(response: LoginResponse) {
        if (!isAccepted(response) || isCompletingLogin) return
        isCompletingLogin = true
        PrefMethods.saveUserToken(prefMain, response.data?.accessToken.orEmpty())
        PrefMethods.saveUserData(prefMain, response.data?.user)
        user = response.data?.user
        viewModelScope.launch {
            coroutineScope {
                launch { cacheProfile() }
                launch { registerPushToken(user?.userId.toString()) }
            }
            loginCompleted.value = true
        }
    }

    /**
     * `GET /employee/me` right after login, so the dashboard opens with the right tabs. If it fails
     * the dashboard refreshes the profile itself.
     */
    private suspend fun cacheProfile() {
        try {
            withTimeout(PROFILE_TIMEOUT_MS) {
                val response = authRepo.empDetails()
                if (response.code == ValConstants.SUCCESS_CODE) {
                    PrefMethods.saveEmpDetailResponse(prefMain, response.data)
                }
            }
        } catch (e: TimeoutCancellationException) {
            Timber.w(e, "Profile fetch after login timed out")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Profile fetch after login failed")
        }
    }

    /** Login must finish whether the token uploads, fails, or takes too long. */
    private suspend fun registerPushToken(employeeId: String) {
        try {
            withTimeout(NOTIFICATION_TOKEN_TIMEOUT_MS) {
                val params = JsonObject().apply {
                    addProperty("employee_id", employeeId)
                    addProperty("token", fetchPushToken())
                    addProperty("device_type", ValConstants.ANDROID)
                }
                authRepo.sendNotificationToken(params)
            }
        } catch (e: TimeoutCancellationException) {
            Timber.w(e, "Push token registration timed out")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Push token registration failed")
        }
    }

    private suspend fun firebasePushToken(): String = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: IllegalStateException("No push token"))
            }
        }
    }

    fun requestLoginWithOtp() {
        val params = JsonObject().apply {
            addProperty("email", userEmailId.get().toString().trim())
        }
        executeApiCall(
            apiCall = {authRepo.requestOTP(params)},
            liveData = requestOtpResponse,
        )
    }

    fun verifyLoginWithOtp(otp:String) {
        val params = JsonObject().apply {
            addProperty("email", userEmailId.get().orEmpty().trim())
            addProperty("otp", otp)
        }
        executeApiCall(
            apiCall = {authRepo.verifyOTP(params)},
            liveData = loginResponse,
            onSuccess = { response -> onLoginAnswered(response) }
        )
    }

    private companion object {
        const val NOTIFICATION_TOKEN_TIMEOUT_MS = 10_000L
        const val PROFILE_TIMEOUT_MS = 10_000L
    }
}
