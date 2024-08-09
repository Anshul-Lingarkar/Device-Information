package com.privorotest.deviceinformation

import androidx.lifecycle.LiveData
import com.privorotest.deviceinformation.model.NetworkData

interface BaseApplicationContract {
    interface ViewModelContract {
        fun updateNetworkData()
        fun requestLocationUpdates()
    }

    interface ViewContract {
    }
}