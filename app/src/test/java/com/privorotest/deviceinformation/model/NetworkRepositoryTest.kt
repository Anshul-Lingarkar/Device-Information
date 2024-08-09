package com.privorotest.deviceinformation.model

import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.argumentCaptor
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

class NetworkRepositoryTest {

    private lateinit var context: Context
    private lateinit var locationManager: LocationManager
    private lateinit var repository: NetworkRepository

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        locationManager = mock(LocationManager::class.java)
        repository = NetworkRepository(context)
    }

    @Test
    fun testGetWifiIpAddress() {
        val loopbackAddress = createInetAddress("127.0.0.1", true)
        val validAddress = createInetAddress("192.168.1.1", false)
        val wifiInterface = createNetworkInterface("wlan0", listOf(loopbackAddress, validAddress))
        val ethInterface = createNetworkInterface("eth0", listOf(loopbackAddress))
        val interfaces = listOf(wifiInterface, ethInterface)

        mockStatic(NetworkInterface::class.java).use { mockedNetworkInterface ->
            `when`(NetworkInterface.getNetworkInterfaces()).thenReturn(
                Collections.enumeration(
                    interfaces
                )
            )

            val ipAddress = repository.getWifiIpAddress()
            assertEquals("192.168.1.1", ipAddress)
        }
    }

    @Test
    fun testNoWifiInterface() {
        val ethAddress = createInetAddress("10.0.0.1", false)
        val ethInterface = createNetworkInterface("eth0", listOf(ethAddress))
        val interfaces = listOf(ethInterface)

        mockStatic(NetworkInterface::class.java).use { mockedNetworkInterface ->
            `when`(NetworkInterface.getNetworkInterfaces()).thenReturn(
                Collections.enumeration(
                    interfaces
                )
            )

            val ipAddress = repository.getWifiIpAddress()
            assertNull(ipAddress)
        }
    }

    @Test
    fun testNoValidAddress() {
        val loopbackAddress = createInetAddress("127.0.0.1", true)
        val wifiInterface = createNetworkInterface("wlan0", listOf(loopbackAddress))
        val interfaces = listOf(wifiInterface)

        mockStatic(NetworkInterface::class.java).use { mockedNetworkInterface ->
            `when`(NetworkInterface.getNetworkInterfaces()).thenReturn(
                Collections.enumeration(
                    interfaces
                )
            )

            val ipAddress = repository.getWifiIpAddress()
            assertNull(ipAddress)
        }
    }

    @Test
    fun testException() {
        mockStatic(NetworkInterface::class.java).use { mockedNetworkInterface ->
            `when`(NetworkInterface.getNetworkInterfaces()).thenThrow(RuntimeException::class.java)

            val ipAddress = repository.getWifiIpAddress()
            assertNull(ipAddress)
        }
    }

    @Test
    fun testGetCurrentLocationNoProvidersEnabled() {
        `when`(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(false)
        `when`(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(false)
        `when`(ContextCompat.getSystemService(context, LocationManager::class.java)).thenReturn(
            locationManager
        )

        val result = repository.getCurrentLocation()
        assertNull(result)
    }

    @Test
    fun testGetCurrentLocationProvidersEnabled() {
        `when`(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true)
        `when`(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(true)
        `when`(ContextCompat.getSystemService(context, LocationManager::class.java)).thenReturn(
            locationManager
        )

        val result = repository.getCurrentLocation()
        assertNull(result)
    }

    @Test
    fun testGetCurrentLocationNoLastKnownLocation() {
        `when`(locationManager.getProviders(true)).thenReturn(listOf(LocationManager.GPS_PROVIDER))
        `when`(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)).thenReturn(null)
        `when`(ContextCompat.getSystemService(context, LocationManager::class.java)).thenReturn(
            locationManager
        )

        val result = repository.getCurrentLocation()
        assertNull(result)
    }

    @Test
    fun testRequestLocationUpdates() {
        val locationListenerCaptor = argumentCaptor<android.location.LocationListener>()
        `when`(ContextCompat.getSystemService(context, LocationManager::class.java)).thenReturn(
            locationManager
        )

        val location = mock(Location::class.java).apply {
            `when`(latitude).thenReturn(12.34)
            `when`(longitude).thenReturn(56.78)
        }

        val locationCallback: (Location) -> Unit = { loc ->
            assertEquals(12.34, loc.latitude, 0.01)
            assertEquals(56.78, loc.longitude, 0.01)
        }

        repository.requestLocationUpdates(locationCallback)

        verify(locationManager, times(2)).requestLocationUpdates(
            anyString(), eq(10000L), eq(10f), locationListenerCaptor.capture()
        )

        locationListenerCaptor.allValues.forEach { listener ->
            listener.onLocationChanged(location)
        }
    }

    @Test
    fun testGetPublicIpAddressException() = runBlockingTest {
        val networkRepository = mock(NetworkRepository(context)::class.java)
        doThrow(RuntimeException("Failed to fetch IP")).`when`(networkRepository)
            .readTextFromUrl("https://api.ipify.org?format=text")

        val result = networkRepository.getPublicIpAddress()
        assertNull(result)
    }

    @Test
    fun testGetCurrentLocationLoop() {
        val location1 = mock(Location::class.java).apply {
            `when`(latitude).thenReturn(12.34)
            `when`(longitude).thenReturn(56.78)
        }
        val location2 = mock(Location::class.java).apply {
            `when`(latitude).thenReturn(90.12)
            `when`(longitude).thenReturn(45.67)
        }
        val providers = listOf("provider1", "provider2")
        `when`(locationManager.getProviders(true)).thenReturn(providers)
        `when`(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true)
        `when`(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(true)
        `when`(locationManager.getLastKnownLocation("provider1")).thenReturn(null)
        `when`(locationManager.getLastKnownLocation("provider2")).thenReturn(location2)
        `when`(ContextCompat.getSystemService(context, LocationManager::class.java)).thenReturn(
            locationManager
        )

        val result = repository.getCurrentLocation()
        result?.latitude?.let { assertEquals(90.12, it, 0.01) }
        result?.longitude?.let { assertEquals(45.67, it, 0.01) }
    }

    @Test
    fun testGetCurrentLocationNoLocation() {
        val providers = listOf("provider1", "provider2")
        `when`(locationManager.getProviders(true)).thenReturn(providers)
        `when`(locationManager.getLastKnownLocation("provider1")).thenReturn(null)
        `when`(locationManager.getLastKnownLocation("provider2")).thenReturn(null)
        `when`(ContextCompat.getSystemService(context, LocationManager::class.java)).thenReturn(
            locationManager
        )

        val result = repository.getCurrentLocation()
        assertNull(result)
    }

    @Test
    fun testReadTextFromUrlMethod() {
        assertEquals(
            "98.177.81.76",
            repository.readTextFromUrl("https://api.ipify.org?format=text")
        )
    }

    private fun createInetAddress(hostAddress: String, isLoopback: Boolean): InetAddress {
        val inetAddress = mock(Inet4Address::class.java)
        `when`(inetAddress.hostAddress).thenReturn(hostAddress)
        `when`(inetAddress.isLoopbackAddress).thenReturn(isLoopback)
        return inetAddress
    }

    private fun createNetworkInterface(
        name: String,
        addresses: List<InetAddress>
    ): NetworkInterface {
        val networkInterface = mock(NetworkInterface::class.java)
        `when`(networkInterface.name).thenReturn(name)
        `when`(networkInterface.inetAddresses).thenReturn(Collections.enumeration(addresses))
        return networkInterface
    }
}
