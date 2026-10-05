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

    /**
     * The launcher icon tapped while the app is already open in the background. The splash is
     * then started on top of the user's screen; it should step aside so they return to where
     * they were, not to a new Dashboard. Notification taps use their own actions and still route.
     */
    fun isRelaunchOverOpenTask(isTaskRoot: Boolean, action: String?, categories: Set<String>?): Boolean =
        !isTaskRoot && action == ACTION_MAIN && categories.orEmpty().contains(CATEGORY_LAUNCHER)

    // Intent.ACTION_MAIN / Intent.CATEGORY_LAUNCHER, spelled out so this stays free of Android.
    private const val ACTION_MAIN = "android.intent.action.MAIN"
    private const val CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER"
}
