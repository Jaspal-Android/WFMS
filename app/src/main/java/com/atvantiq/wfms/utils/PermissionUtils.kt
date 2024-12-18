package com.atvantiq.wfms.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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

