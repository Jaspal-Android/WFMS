package com.atvantiq.wfms.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
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

}

