package com.privorotest.deviceinformation.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.io.File

class FileUtilsTest {

    //private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var context: Context
    private lateinit var externalFilesDir: File

    @Before
    fun setUp() {
        context = Mockito.mock(Context::class.java)
        externalFilesDir = File("/tmp") // Mock external files dir

    }

    @Test
    fun testWriteDataToCsv() {
        val data = "192.168.1.1,12.34,56.78,01:01:2024 12:00:00,01:01:2024 17:00:00"
        FileUtils.writeDataToCsv(context, data)

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "network_data.csv")
        assertTrue(file.exists())
    }

    /*@Test
    fun testGetCsvFileUri() {
        val file = FileUtils.getCsvFileUri(context)
        assertTrue(file.exists())
    }*/

    @Test
    fun testWriteDataToCsv_NewFile() {
        val data = "192.168.1.1,12.34,56.78,01:01:2024 12:00:00,01:01:2024 17:00:00"
        FileUtils.writeDataToCsv(context, data)

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "network_data.csv")
        assertTrue(file.exists())

        val fileContent = file.readText()
        assertTrue(fileContent.contains(data))
    }

    @Test
    fun testWriteDataToCsv_AppendToFile() {
        Mockito.`when`(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)).thenReturn(externalFilesDir)
        val data = "192.168.1.1,12.34,56.78,01:01:2024 12:00:00,01:01:2024 17:00:00"
        val file = File(externalFilesDir, "network_data.csv")
        file.writeText("IP Address, Latitude, Longitude, Local Time, UTC Time\n") // Create file with header

        FileUtils.writeDataToCsv(context, data)

        val fileContent = file.readText()
        assertTrue(fileContent.contains(data))
    }

    @Test
    fun testGetCsvFile() {
        // Mock the context to return the externalFilesDir
        Mockito.`when`(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)).thenReturn(externalFilesDir)

        // Create the file
        val file = File(externalFilesDir, "network_data.csv")

        // Ensure the file exists for testing
        file.createNewFile()

        // Get the file using the method
        val csvFile = FileUtils.getCsvFile(context)

        // Verify the file exists
        assertTrue(csvFile != null)
        assertTrue(csvFile!!.exists())
        assertTrue(csvFile.name == "network_data.csv")
    }
}