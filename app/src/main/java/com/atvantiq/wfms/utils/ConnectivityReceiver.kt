package com.atvantiq.wfms.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class ConnectivityReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        connectivityReceiverListener?.onNetworkConnectionChanged(isNetworkAvailable(context))
    }

    interface ConnectivityReceiverListener {
        fun onNetworkConnectionChanged(isConnected: Boolean)
    }

    companion object {
        var connectivityReceiverListener: ConnectivityReceiverListener? = null

        fun isNetworkAvailable(context: Context?): Boolean {
            val connectivityManager =
                context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return false
            return try {
                hasInternetCapability(
                    connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                )
            } catch (e: SecurityException) {
                false
            }
        }

        /**
         * Any network that can reach the internet counts: Wi-Fi, cellular, Ethernet, VPN or a
         * tethered connection. Checking only Wi-Fi and cellular reported the others as offline
         * and stopped every request before it was sent.
         */
        fun hasInternetCapability(capabilities: NetworkCapabilities?): Boolean =
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}
