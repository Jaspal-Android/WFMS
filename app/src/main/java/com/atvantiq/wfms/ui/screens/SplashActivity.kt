package com.atvantiq.wfms.ui.screens

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.atvantiq.wfms.BuildConfig
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivitySimple
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.models.loginResponse.Permission
import com.atvantiq.wfms.ui.screens.admin.SharedDashboardActivity
import com.atvantiq.wfms.ui.screens.login.LoginActivity
import com.atvantiq.wfms.utils.Utils
import com.ssas.jibli.data.prefs.PrefMethods
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class SplashActivity : BaseActivitySimple() {

    companion object {
        const val ACTION_LOCATION_NOTIFICATION = "com.atvantiq.wfms.action.LOCATION_NOTIFICATION"
        const val ACTION_PUSH_NOTIFICATION = "com.atvantiq.wfms.action.PUSH_NOTIFICATION"

        // The splash stays up only as long as reading the session takes, but never flashes past
        // faster than this. It used to wait a fixed 2 s on every cold start. 0 routes as soon as
        // the session is read.
        private const val MIN_SPLASH_VISIBLE_MS = 600L
    }

    private class Destination(val target: SplashTarget, val permissions: List<Permission>?)

    private var routed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        checkAppVersion()
        // A tapped notification goes straight through; a normal launch keeps the brand visible
        // for a moment.
        route(minVisibleMs = if (isNotificationLaunch()) 0L else MIN_SPLASH_VISIBLE_MS)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isNotificationLaunch()) {
            route(minVisibleMs = 0L)
        }
    }

    private fun isNotificationLaunch(): Boolean {
        return intent?.action in setOf(ACTION_LOCATION_NOTIFICATION, ACTION_PUSH_NOTIFICATION)
    }

    private fun checkAppVersion() {
        findViewById<TextView>(R.id.versionText).text = "V${BuildConfig.VERSION_NAME}"
    }

    /**
     * Reads the saved session off the main thread and opens the right screen. The secure
     * preferences go through the Keystore, which can take hundreds of milliseconds on the first
     * access; doing that on the main thread froze the splash. lifecycleScope cancels on destroy,
     * so this can never fire on a finished activity.
     */
    private fun route(minVisibleMs: Long) {
        lifecycleScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            val destination = withContext(Dispatchers.IO) { resolveDestination() }
            val remaining = minVisibleMs - (SystemClock.elapsedRealtime() - startedAt)
            if (remaining > 0) delay(remaining)
            open(destination)
        }
    }

    private fun resolveDestination(): Destination {
        val token: String? = try {
            PrefMethods.getUserToken(prefMain)
        } catch (e: Exception) {
            Log.e("SplashActivity", "Failed to read secure prefs even after recovery", e)
            null
        }
        val user = if (token.isNullOrBlank()) {
            null
        } else {
            try {
                PrefMethods.getUserData(prefMain)
            } catch (e: Exception) {
                Log.e("SplashActivity", "Failed to read user data", e)
                null
            }
        }
        return Destination(SplashRouting.targetFor(token, user), user?.permissions)
    }

    private fun open(destination: Destination) {
        if (routed) return // e.g. a notification tap while the normal launch was still resolving
        routed = true

        val target = when (destination.target) {
            SplashTarget.LOGIN -> {
                Utils.jumpActivity(this, LoginActivity::class.java)
                finish()
                return
            }
            SplashTarget.EMPLOYEE_DASHBOARD -> DashboardActivity::class.java
            SplashTarget.ADMIN_DASHBOARD -> SharedDashboardActivity::class.java
        }
        Utils.jumpActivityWithData(this, target, Bundle().apply {
            putParcelableArrayList(SharingKeys.ROLE_PERMISSIONS, ArrayList(destination.permissions.orEmpty()))
        })
        finish()
    }
}
