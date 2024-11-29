package com.atvantiq.wfms.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log


class ConnectivityReceiver : BroadcastReceiver() {
	
	override fun onReceive(context: Context, arg1: Intent) {
		if (connectivityReceiverListener != null) {
			connectivityReceiverListener!!.onNetworkConnectionChanged(isNetworkAvailable(context))
		}
	}
	
	
	interface ConnectivityReceiverListener {
		fun onNetworkConnectionChanged(isConnected: Boolean)
	}
	
	companion object {
		var connectivityReceiverListener: ConnectivityReceiverListener? = null
		
		fun isNetworkAvailable(context: Context?): Boolean {
			if (context == null)
				return false
			val connectivityManager =
				context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
			if (connectivityManager != null) {
				if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
					val capabilities =
						connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
					if (capabilities != null) {
						if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
							return true
						} else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
							return true
						}
					}
				} else {
					try {
						val activeNetworkInfo = connectivityManager.activeNetworkInfo
						if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
							Log.i("update_statut", "Network is available : true")
							return true
						}
					} catch (e: Exception) {
						Log.i("update_statut", "" + e.message)
					}
					
				}
			}
			Log.i("update_statut", "Network is available : FALSE ")
			return false
		}
	}
}