package com.example.weatherapp.core.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkUtils {

    // Checks whether the device is connected to the internet using available transports like WIFI,
    // Mobile Data or Ethernet and returns true otherwise returns false.
    fun isInternetAvailable(context: Context): Boolean {

        // Get system service to check network status
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Get the active network
        val network = connectivityManager.activeNetwork ?: return false

        // Get details of the active network
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        // Check if the network has internet access via this transport types and returns true otherwise false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}