package com.privorotest.deviceinformation.model

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URL
import java.util.*

class NetworkRepository(val context: Context): NetworkRepositoryContract {

    override fun getWifiIpAddress(): String? {
        try {
            val interfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                if (intf.name.contains("wlan") || intf.name.contains("wifi")) {
                    val addresses: Enumeration<InetAddress> = intf.inetAddresses
                    for (addr in addresses) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            return addr.hostAddress
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }

    override suspend fun getPublicIpAddress(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val response = readTextFromUrl("https://api.ipify.org?format=text")
                response
            } catch (ex: Exception) {
                ex.printStackTrace()
                null
            }
        }
    }

    override fun readTextFromUrl(urlString: String): String {
        return URL(urlString).readText()
    }

    override fun getCurrentLocation(): Location? {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permissions not granted then return null
            return null
        }

        val locationManager = ContextCompat.getSystemService(context, LocationManager::class.java) ?: return null
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            return null
        }

        val providers = locationManager.getProviders(true)
        for (provider in providers) {
            val location = locationManager.getLastKnownLocation(provider)
            if (location != null) {
                return location
            }
        }
        return null
    }

    override fun requestLocationUpdates(locationCallback: (Location) -> Unit) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permissions not granted then don't return anything
            return
        }

        val locationManager = ContextCompat.getSystemService(context, LocationManager::class.java) ?: return
        val locationListener = android.location.LocationListener { location ->
            locationCallback(location)
        }
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            10000L, // 10 seconds
            10f, // 10 meters
            locationListener
        )

        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            10000L, // 10 seconds
            10f, // 10 meters
            locationListener
        )
    }
}
