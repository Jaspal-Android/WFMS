package com.atvantiq.wfms.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log

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
            if (context == null) {
                println("Context is null")
                return false
            }
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            if (connectivityManager == null) {
                println("ConnectivityManager is null")
                return false
            }
            println("SDK: ${Build.VERSION.SDK_INT}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                println("Network: $network")
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                println("Capabilities: $capabilities")
                if (capabilities != null) {
                    println("Cellular: ${capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)}")
                    println("Wi-Fi: ${capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)}")
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        println("Cellular network available")
                        return true
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        println("Wi-Fi network available")
                        return true
                    }
                } else {
                    println("Capabilities is null")
                }
            } else {
                try {
                    val activeNetworkInfo = connectivityManager.activeNetworkInfo
                    println("NetworkInfo: $activeNetworkInfo")
                    if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                        println("Legacy network available")
                        return true
                    } else {
                        println("NetworkInfo is null or not connected")
                    }
                } catch (e: Exception) {
                    println("Network check error: $e")
                }
            }
            println("No network available")
            return false
        }
    }
}
