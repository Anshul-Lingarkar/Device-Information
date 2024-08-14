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

    private val userNetworkData = MutableLiveData<NetworkData>()
    val networkData: LiveData<NetworkData> get() = userNetworkData
    private val handler = Handler(Looper.getMainLooper())
    private var networkRepository = NetworkRepository(application)

    init {
        val connectivityManager =
            application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder().build()

        connectivityManager.registerNetworkCallback(
            networkRequest,
            object : ConnectivityManager.NetworkCallback() {
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
            val utcTime = getCurrentTimeInTimeZone(UTC_TIME_ZONE)
            val networkData = NetworkData(ipAddress, location, localTime, utcTime)
            // Append data to CSV when network data is updated
            userNetworkData.postValue(networkData)
            appendDataToCsv(networkData)
        }
    }

    override fun requestLocationUpdates() {
        networkRepository.requestLocationUpdates { location ->
            val currentNetworkData = userNetworkData.value
            val updatedNetworkData = currentNetworkData?.copy(location = location)
            // Append data to CSV when location is updated
            updatedNetworkData?.let {
                userNetworkData.postValue(it)
                appendDataToCsv(it)
            }
        }
    }

    private val _shareFileEvent = MutableLiveData<String?>()
    val shareFileEvent: LiveData<String?> get() = _shareFileEvent

    override fun onDownloadButtonClicked() {
        val csvFile = FileUtils.getCsvFile(getApplication<Application>())
        if (csvFile == null || !csvFile.exists() || csvFile.length() == 0L) {
            _shareFileEvent.value = null // No file to share
        } else {
            val csvFileUri = FileUtils.getCsvFileUri(getApplication<Application>())
            _shareFileEvent.value = csvFileUri.toString() // File URI to share
        }
    }

    // Reset the event after it has been handled
    fun resetShareFileEvent() {
        _shareFileEvent.value = null
    }

    suspend fun getIpAddress(): String? {
        val connectivityManager =
            getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
        val sdf = SimpleDateFormat(TIME_FORMAT, Locale.getDefault())
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
        val utcTime = getCurrentTimeInTimeZone(UTC_TIME_ZONE)
        val currentNetworkData = userNetworkData.value
        val updatedNetworkData = currentNetworkData?.copy(localTime = localTime, utcTime = utcTime)
        updatedNetworkData?.let {
            userNetworkData.postValue(it)
        }
    }

    private fun appendDataToCsv(networkData: NetworkData) {
        val data =
            "${networkData.ipAddress ?: "Not Available"},${networkData.location?.latitude ?: "Not Available"},${networkData.location?.longitude ?: "Not Available"},${networkData.localTime},${networkData.utcTime}"
        FileUtils.writeDataToCsv(getApplication(), data)
    }

    companion object {
        const val TIME_FORMAT = "MM:dd:yyyy HH:mm:ss"
        const val UTC_TIME_ZONE = "UTC"
        const val CSV_FILE_NAME = "network_data.csv"
    }
}

