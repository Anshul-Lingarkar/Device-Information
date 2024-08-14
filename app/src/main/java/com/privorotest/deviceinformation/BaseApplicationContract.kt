package com.privorotest.deviceinformation

interface BaseApplicationContract {
    interface ViewModelContract {
        fun updateNetworkData()
        fun requestLocationUpdates()
        fun onDownloadButtonClicked()
    }

    interface ViewContract {
    }
}