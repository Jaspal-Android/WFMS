package com.atvantiq.wfms.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import java.util.*

object PermissionUtils {

	var LOCATION_PERMISSIONS =arrayOf(
		Manifest.permission.ACCESS_FINE_LOCATION,
		Manifest.permission.ACCESS_COARSE_LOCATION
	)

	private fun checkPermissionGranted(context: Context, permissions: Array<String>): Boolean {
		val deniedPermissions = ArrayList<String>()
		for (permission in permissions) {
			if (ActivityCompat.checkSelfPermission(
					context,
					permission
				) == PackageManager.PERMISSION_DENIED
			) {
				deniedPermissions.add(permission)
			}
		}
		return deniedPermissions.isEmpty()
	}

	/** POST_NOTIFICATIONS on Android 13+, nothing before it. */
	fun notificationPermissions(): Array<String> =
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			arrayOf(Manifest.permission.POST_NOTIFICATIONS)
		} else {
			emptyArray()
		}

	/** Background location only exists as a runtime permission from Android 10. */
	fun hasBackgroundLocationPermission(context: Context): Boolean =
		Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
			ActivityCompat.checkSelfPermission(
				context,
				Manifest.permission.ACCESS_BACKGROUND_LOCATION
			) == PackageManager.PERMISSION_GRANTED

	/** True when every permission in [required] is granted in a permission-request [results] map. */
	fun areGranted(results: Map<String, Boolean>, required: Array<String>): Boolean =
		required.all { results[it] == true }

	fun hasLocationPermissions(context: Context): Boolean {
		return checkPermissionGranted(
			context,
			arrayOf(
				Manifest.permission.ACCESS_FINE_LOCATION,
				Manifest.permission.ACCESS_COARSE_LOCATION
			)
		)
	}

	/** How a permission request ended, judged on the location permissions alone. */
	enum class LocationOutcome { GRANTED, DENIED_PERMANENTLY, DENIED_CAN_RETRY }

	/**
	 * Reads the [results] of a location request: granted, or denied for good (the system will no
	 * longer show its prompt, only Settings can help), or denied but the user may be asked again.
	 * Other permissions in the same request (notifications) do not matter for the outcome.
	 */
	fun locationOutcome(results: Map<String, Boolean>, shouldShowRationale: (String) -> Boolean): LocationOutcome = when {
		areGranted(results, LOCATION_PERMISSIONS) -> LocationOutcome.GRANTED
		LOCATION_PERMISSIONS.none { shouldShowRationale(it) } -> LocationOutcome.DENIED_PERMANENTLY
		else -> LocationOutcome.DENIED_CAN_RETRY
	}

	/**
	 * The same for "Allow all the time". Once the user has declined it twice (backing out of its
	 * Settings page counts), Android answers "denied" at once without showing anything.
	 */
	fun backgroundLocationOutcome(granted: Boolean, shouldShowRationale: (String) -> Boolean): LocationOutcome = when {
		granted -> LocationOutcome.GRANTED
		shouldShowRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION) -> LocationOutcome.DENIED_CAN_RETRY
		else -> LocationOutcome.DENIED_PERMANENTLY
	}

}
