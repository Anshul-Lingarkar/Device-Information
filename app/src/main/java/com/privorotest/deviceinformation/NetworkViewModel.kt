package com.privorotest.deviceinformation

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.privorotest.deviceinformation.model.NetworkData
import com.privorotest.deviceinformation.model.NetworkRepository
import com.privorotest.deviceinformation.utils.FileUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NetworkViewModel(application: Application) : AndroidViewModel(application),
    BaseApplicationContract.ViewModelContract {

    private val _networkData = MutableLiveData<NetworkData>()
    val networkData: LiveData<NetworkData> get() = _networkData
    private val handler = Handler(Looper.getMainLooper())

    var networkRepository = NetworkRepository(application)

    init {
        val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder().build()

        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                updateNetworkData()
            }

            override fun onLost(network: android.net.Network) {
                updateNetworkData()
            }
        })

        updateNetworkData()
        startTimer()
        requestLocationUpdates()
    }

    override fun updateNetworkData() {
        viewModelScope.launch {
            val ipAddress = getIpAddress()
            val location = networkRepository.getCurrentLocation()
            val localTime = getCurrentTimeInTimeZone(TimeZone.getDefault().id)
            val utcTime = getCurrentTimeInTimeZone("UTC")
            val networkData = NetworkData(ipAddress, location, localTime, utcTime)
            _networkData.postValue(networkData)
            appendDataToCsv(networkData)  // Append data to CSV when data is updated
        }
    }

    override fun requestLocationUpdates() {
        networkRepository.requestLocationUpdates { location ->
            val currentNetworkData = _networkData.value
            val updatedNetworkData = currentNetworkData?.copy(location = location)
            updatedNetworkData?.let {
                _networkData.postValue(it)
                appendDataToCsv(it) // Append data to CSV when location is updated
            }
        }
    }

    suspend fun getIpAddress(): String? {
        val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        return if (networkCapabilities != null) {
            when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    networkRepository.getWifiIpAddress()
                }
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    networkRepository.getPublicIpAddress()
                }
                else -> {
                    null
                }
            }
        } else {
            null
        }
    }

    private fun getCurrentTimeInTimeZone(timeZoneId: String): String {
        val sdf = SimpleDateFormat("MM:dd:yyyy HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone(timeZoneId)
        return sdf.format(Date())
    }

    private fun startTimer() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                updateTime()
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun updateTime() {
        val localTime = getCurrentTimeInTimeZone(TimeZone.getDefault().id)
        val utcTime = getCurrentTimeInTimeZone("UTC")
        val currentNetworkData = _networkData.value
        val updatedNetworkData = currentNetworkData?.copy(localTime = localTime, utcTime = utcTime)
        updatedNetworkData?.let {
            _networkData.postValue(it)
        }
    }

    private fun appendDataToCsv(networkData: NetworkData) {
        val data = "${networkData.ipAddress ?: "N/A"},${networkData.location?.latitude ?: "N/A"},${networkData.location?.longitude ?: "N/A"},${networkData.localTime},${networkData.utcTime}"
        FileUtils.writeDataToCsv(getApplication(), data)
    }
}

