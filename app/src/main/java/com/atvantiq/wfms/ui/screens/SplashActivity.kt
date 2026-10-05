package com.atvantiq.wfms.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import com.atvantiq.wfms.BuildConfig
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.databinding.ActivitySplashBinding
import com.atvantiq.wfms.ui.screens.login.LoginActivity
import com.atvantiq.wfms.utils.Utils
import dagger.hilt.android.AndroidEntryPoint
import com.atvantiq.wfms.utils.applySystemBarsAndImePadding

@AndroidEntryPoint
class SplashActivity : BaseActivity<ActivitySplashBinding, SplashVM>() {

    companion object {
        const val ACTION_LOCATION_NOTIFICATION = "com.atvantiq.wfms.action.LOCATION_NOTIFICATION"
        const val ACTION_PUSH_NOTIFICATION = "com.atvantiq.wfms.action.PUSH_NOTIFICATION"

        // The splash stays up only as long as reading the session takes, but never flashes past
        // faster than this. 0 routes as soon as the session is read.
        private const val MIN_SPLASH_VISIBLE_MS = 600L
    }

    private var routed = false

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_splash, SplashVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        if (SplashRouting.isRelaunchOverOpenTask(isTaskRoot, intent?.action, intent?.categories)) {
            finish()
            return
        }
        enableEdgeToEdge()
        binding.main.applySystemBarsAndImePadding()
        binding.versionText.text = getString(R.string.version_label, BuildConfig.VERSION_NAME)
        // A tapped notification goes straight through; a normal launch keeps the brand visible
        // for a moment.
        viewModel.resolve(minVisibleMs = if (isNotificationLaunch()) 0L else MIN_SPLASH_VISIBLE_MS)
    }

    override fun subscribeToEvents(vm: SplashVM) {
        vm.destination.observe(this) { destination -> open(destination) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isNotificationLaunch()) {
            viewModel.resolve(minVisibleMs = 0L)
        }
    }

    private fun isNotificationLaunch(): Boolean {
        return intent?.action in setOf(ACTION_LOCATION_NOTIFICATION, ACTION_PUSH_NOTIFICATION)
    }

    private fun open(destination: SplashTarget) {
        if (routed) return
        routed = true
        val screen = when (destination) {
            SplashTarget.LOGIN -> LoginActivity::class.java
            SplashTarget.DASHBOARD -> DashboardActivity::class.java
        }
        Utils.jumpActivity(this, screen)
        finish()
    }
}
