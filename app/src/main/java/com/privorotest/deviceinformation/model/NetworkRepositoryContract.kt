package com.privorotest.deviceinformation.model

import android.location.Location

interface NetworkRepositoryContract {
    fun getWifiIpAddress(): String?
    suspend fun getPublicIpAddress(): String?
    fun readTextFromUrl(urlString: String): String
    fun getCurrentLocation(): Location?
    fun requestLocationUpdates(locationCallback: (Location) -> Unit)
}