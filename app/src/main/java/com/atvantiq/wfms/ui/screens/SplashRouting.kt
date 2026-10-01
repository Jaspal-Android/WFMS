package com.atvantiq.wfms.ui.screens

import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.models.loginResponse.Permission
import com.atvantiq.wfms.models.loginResponse.User

enum class SplashTarget { LOGIN, EMPLOYEE_DASHBOARD, ADMIN_DASHBOARD }

/** The screen to open and the permissions it is opened with. */
data class SplashDestination(val target: SplashTarget, val permissions: List<Permission>)

/** Where the app opens, decided from the saved session. Kept free of Android so it is unit tested. */
object SplashRouting {

    /**
     * No token, or a token without saved user data (a half-written session), means login.
     * Otherwise employees get their dashboard and every other role the shared admin dashboard.
     */
    fun targetFor(token: String?, user: User?): SplashTarget = when {
        token.isNullOrBlank() -> SplashTarget.LOGIN
        user == null -> SplashTarget.LOGIN
        user.role.equals(ValConstants.ROLE_EMPLOYEE, ignoreCase = true) -> SplashTarget.EMPLOYEE_DASHBOARD
        else -> SplashTarget.ADMIN_DASHBOARD
    }
}
