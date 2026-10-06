package com.atvantiq.wfms.utils.permissions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import com.atvantiq.wfms.R
import com.atvantiq.wfms.utils.PermissionUtils
import com.atvantiq.wfms.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import timber.log.Timber

/**
 * The one place that asks for foreground location.
 *
 * Play's Prominent Disclosure policy: the system prompt is only ever shown right after the
 * in-app disclosure, so [request] always shows it first and no screen can launch the prompt on
 * its own. A denial is then explained the same way everywhere: a rationale dialog while the user
 * can still be asked, a "open Settings" dialog once the system will no longer show its prompt.
 *
 * Create it as a field of the Activity or Fragment (the launcher must be registered before the
 * screen starts). [config] is fixed at creation, so a result delivered to a recreated screen is
 * still handled.
 */
class LocationPermissionDelegate(
    caller: ActivityResultCaller,
    private val contextProvider: () -> Context,
    private val shouldShowRationale: (String) -> Boolean,
    private val config: Config
) {

    /** What to do after a rationale dialog's Retry button. */
    enum class Retry {
        /** Show the disclosure and the system prompt again. */
        REQUEST_AGAIN,

        /** Open the app's Settings page. */
        OPEN_SETTINGS
    }

    /** The in-app disclosure shown before the system prompt. */
    class Disclosure(@StringRes val title: Int, @StringRes val message: Int)

    /**
     * @property disclosure the in-app disclosure shown before the system prompt.
     * @property includeNotifications also ask for notifications (Android 13+) in the same prompt.
     * @property rationaleFirst when the system says the user should be told why, show that
     * dialog instead of the disclosure and the prompt.
     * @property onGranted runs when location is available; a request may pass its own instead.
     * @property onAbandoned runs when the user gives up (denied for good, or cancelled the rationale).
     */
    class Config(
        val disclosure: Disclosure,
        val retry: Retry = Retry.OPEN_SETTINGS,
        val includeNotifications: Boolean = false,
        val rationaleFirst: Boolean = false,
        val onGranted: (() -> Unit)? = null,
        val onAbandoned: () -> Unit = {}
    )

    private var pendingAction: (() -> Unit)? = null

    /** Set once this request has asked Android to upgrade "Approximate" to precise. */
    private var preciseAsked = false

    private val launcher = caller.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> onResult(results) }

    /**
     * Runs [action] (or [Config.onGranted]) once location is granted: now if it already is,
     * otherwise after the disclosure and the system prompt.
     */
    fun request(action: (() -> Unit)? = null) {
        pendingAction = action
        preciseAsked = false
        val context = contextProvider()
        when {
            PermissionUtils.hasLocationPermissions(context) -> granted()
            PermissionUtils.hasApproximateLocationOnly(context) -> askForPrecise()
            config.rationaleFirst && PermissionUtils.LOCATION_PERMISSIONS.any(shouldShowRationale) -> showRationale()
            else -> discloseThenAsk()
        }
    }

    private fun discloseThenAsk() {
        val context = contextProvider()
        Utils.showLocationDisclosureDialog(
            context,
            context.getString(config.disclosure.title),
            context.getString(config.disclosure.message)
        ) {
            launcher.launch(PermissionUtils.LOCATION_PERMISSIONS + notificationPermissions())
        }
    }

    private fun notificationPermissions(): Array<String> =
        if (config.includeNotifications) PermissionUtils.notificationPermissions() else emptyArray()

    private fun onResult(results: Map<String, Boolean>) {
        when (PermissionUtils.locationOutcome(results, shouldShowRationale)) {
            PermissionUtils.LocationOutcome.GRANTED -> granted()
            // The pending action is kept until precise is granted or the user cancels.
            PermissionUtils.LocationOutcome.APPROXIMATE_ONLY -> askForPrecise()
            PermissionUtils.LocationOutcome.DENIED_PERMANENTLY -> {
                abandon()
                showDeniedPermanently()
            }
            // The pending action is kept: Retry resumes it, Cancel releases it.
            PermissionUtils.LocationOutcome.DENIED_CAN_RETRY -> showRationale()
        }
    }

    /**
     * Only "Approximate" is granted, but attendance and the route need the precise location.
     * Explains that, then asks again: Android shows its own "Change to precise location" prompt.
     * Once Android won't show it any more, Settings is the only way, so the dialog says where.
     */
    private fun askForPrecise() {
        val context = contextProvider()
        val canAsk = !preciseAsked || shouldShowRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.precise_location_title)
            .setMessage(if (canAsk) R.string.precise_location_message else R.string.precise_location_settings_message)
            .setPositiveButton(if (canAsk) R.string.continue_label else R.string.open_settings) { _, _ ->
                if (canAsk) {
                    preciseAsked = true
                    launcher.launch(PermissionUtils.LOCATION_PERMISSIONS + notificationPermissions())
                } else {
                    abandon()
                    context.openAppSettings()
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> abandon() }
            .setOnCancelListener { abandon() }
            .show()
    }

    private fun granted() {
        val action = pendingAction ?: config.onGranted
        pendingAction = null
        action?.invoke()
    }

    private fun abandon() {
        pendingAction = null
        config.onAbandoned()
    }

    private fun showRationale() {
        val context = contextProvider()
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.location_permission_rationale)
            .setPositiveButton(R.string.retry) { _, _ ->
                when (config.retry) {
                    Retry.REQUEST_AGAIN -> discloseThenAsk()
                    Retry.OPEN_SETTINGS -> context.openAppSettings()
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> abandon() }
            .setOnCancelListener { abandon() }
            .show()
    }

    private fun showDeniedPermanently() {
        val context = contextProvider()
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.permission_denied)
            .setMessage(R.string.permission_denied_permanently)
            .setPositiveButton(R.string.open_settings) { _, _ -> context.openAppSettings() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

}

/**
 * Opens this app's page in the system Settings, where a permission denied for good can be turned on.
 * In its own task: otherwise Settings stays on top of this app's task, and reopening the app from
 * the launcher lands on the Settings page.
 */
fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

/**
 * Opens the system list of apps exempt from battery optimisation, where this app can be set to
 * "Unrestricted"/"Not optimised". Some devices don't have that screen; this app's page is the
 * fallback. (Asking directly with REQUEST_IGNORE_BATTERY_OPTIMIZATIONS is restricted by Play.)
 */
fun Context.openBatteryOptimizationSettings() {
    try {
        startActivity(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: ActivityNotFoundException) {
        Timber.w(e, "No battery optimisation settings screen; opening app settings")
        openAppSettings()
    }
}

/**
 * Background ("Allow all the time") location. Android 11+ ignores it when it is asked for
 * together with foreground location, so it is requested on its own, after its own disclosure.
 * A decline is not an error: tracking keeps running as a foreground service, so [onResult] only
 * lets the screen refresh what it shows.
 */
class BackgroundLocationRequest(
    caller: ActivityResultCaller,
    private val contextProvider: () -> Context,
    private val shouldShowRationale: (String) -> Boolean,
    @StringRes private val disclosureTitle: Int,
    @StringRes private val disclosureMessage: Int,
    private val onResult: () -> Unit
) {

    /** Set by a request the user asked for (the Fix button), so a dead end can lead to Settings. */
    private var offerSettingsWhenBlocked = false

    private val launcher = caller.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> onRequestResult(granted) }

    /**
     * Disclosure, then the request. With [offerSettingsIfBlocked] (the user tapped Fix), when
     * Android no longer shows its prompt the user is told and offered Settings instead of nothing
     * happening. An automatic request (after Start Day) leaves a decline alone.
     */
    fun request(offerSettingsIfBlocked: Boolean = false) {
        offerSettingsWhenBlocked = offerSettingsIfBlocked
        val context = contextProvider()
        Utils.showLocationDisclosureDialog(
            context,
            context.getString(disclosureTitle),
            context.getString(disclosureMessage)
        ) {
            launcher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    private fun onRequestResult(granted: Boolean) {
        val outcome = PermissionUtils.backgroundLocationOutcome(granted, shouldShowRationale)
        if (outcome == PermissionUtils.LocationOutcome.DENIED_PERMANENTLY && offerSettingsWhenBlocked) {
            showOpenSettings()
        }
        offerSettingsWhenBlocked = false
        onResult()
    }

    private fun showOpenSettings() {
        val context = contextProvider()
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.background_location_settings_title)
            .setMessage(R.string.background_location_settings_msg)
            .setPositiveButton(R.string.open_settings) { _, _ -> context.openAppSettings() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
