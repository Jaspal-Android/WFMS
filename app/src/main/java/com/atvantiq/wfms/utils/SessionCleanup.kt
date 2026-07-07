package com.atvantiq.wfms.utils

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.atvantiq.wfms.data.prefs.PrefKeys
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.services.LocationTrackingService

object SessionCleanup {

    fun clearForLogout(context: Context, prefMain: SecurePrefMain) {
        prefMain.put(PrefKeys.IS_TRACKING_ACTIVE, false)
        ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?.cancelAll()
        context.stopService(Intent(context, LocationTrackingService::class.java))
        prefMain.deleteAll()
    }
}
