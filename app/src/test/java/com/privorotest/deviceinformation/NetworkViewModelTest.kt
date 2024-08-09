package com.privorotest.deviceinformation
import android.app.Application
import android.content.Context
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.privorotest.deviceinformation.model.NetworkData
import com.privorotest.deviceinformation.model.NetworkRepository
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class NetworkViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockApplication: Application

    @Mock
    private lateinit var mockConnectivityManager: ConnectivityManager

    @Mock
    private lateinit var mockNetworkRepository: NetworkRepository

    private lateinit var networkViewModel: NetworkViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        Mockito.`when`(mockApplication.getSystemService(Context.CONNECTIVITY_SERVICE))
            .thenReturn(mockConnectivityManager)

        networkViewModel = NetworkViewModel(mockApplication)
        networkViewModel.networkRepository = mockNetworkRepository
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testUpdateNetworkData() = runTest {
        // Mock the IP address and location
        Mockito.`when`(mockNetworkRepository.getWifiIpAddress()).thenReturn("192.168.1.1")
        Mockito.`when`(mockNetworkRepository.getCurrentLocation()).thenReturn(Location("mockLocation"))

        // Call the method to test
        networkViewModel.updateNetworkData()

        // Verify if the data was posted correctly
        val networkData = networkViewModel.networkData.value
        assertNotNull(networkData)
        assertEquals("192.168.1.1", networkData?.ipAddress)
        assertNotNull(networkData?.location)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testGetIpAddress() = runTest {
        // Mock the network capabilities
        val mockNetworkCapabilities = Mockito.mock(NetworkCapabilities::class.java)
        Mockito.`when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(true)
        Mockito.`when`(mockConnectivityManager.getNetworkCapabilities(Mockito.any())).thenReturn(mockNetworkCapabilities)

        // Mock the IP address
        Mockito.`when`(mockNetworkRepository.getWifiIpAddress()).thenReturn("192.168.1.1")

        // Call the method to test
        val ipAddress = networkViewModel.getIpAddress()

        // Verify the IP address
        assertEquals("192.168.1.1", ipAddress)
    }
}
