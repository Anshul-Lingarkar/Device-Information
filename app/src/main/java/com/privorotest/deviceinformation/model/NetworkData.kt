package com.privorotest.deviceinformation.model

import android.location.Location

data class NetworkData(
    val ipAddress: String?,
    val location: Location?,
    val localTime: String,
    val utcTime: String
)