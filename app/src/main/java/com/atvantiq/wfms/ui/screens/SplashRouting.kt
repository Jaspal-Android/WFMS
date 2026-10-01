package com.atvantiq.wfms.ui.screens

import com.atvantiq.wfms.models.loginResponse.User

enum class SplashTarget { LOGIN, DASHBOARD }

/** Where the app opens, decided from the saved session. Kept free of Android so it is unit tested. */
object SplashRouting {

    /**
     * No token, or a token without saved user data (a half-written session), means login.
     * Every role opens the same dashboard, which picks its tabs from the role.
     */
    fun targetFor(token: String?, user: User?): SplashTarget = when {
        token.isNullOrBlank() -> SplashTarget.LOGIN
        user == null -> SplashTarget.LOGIN
        else -> SplashTarget.DASHBOARD
    }
}
