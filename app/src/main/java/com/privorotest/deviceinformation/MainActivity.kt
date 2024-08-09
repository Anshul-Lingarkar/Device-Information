package com.privorotest.deviceinformation

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.annotation.RequiresApi
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.privorotest.deviceinformation.utils.FileUtils
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var ipTextView: TextView
    private lateinit var locationTextView: TextView
    private lateinit var datetimeTextView: TextView
    private lateinit var downloadButton: Button
    private val REQUEST_CODE_PERMISSIONS = 101

    private val networkViewModel: NetworkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNecessaryPermissions()
        setContentView(R.layout.activity_main)
        ipTextView = findViewById(R.id.ip_text_view)
        locationTextView = findViewById(R.id.location_text_view)
        datetimeTextView = findViewById(R.id.datetime_text_view)
        downloadButton = findViewById(R.id.download_button)

        networkViewModel.networkData.observe(this, Observer { networkData ->
            ipTextView.text = networkData.ipAddress ?: getString(R.string.ip_default)
            locationTextView.text = networkData.location?.let {
                getString(R.string.location_format, it.latitude, it.longitude)
            } ?: getString(R.string.location_default)
            datetimeTextView.text = getString(R.string.datetime_format, networkData.localTime, networkData.utcTime)
        })

        downloadButton.setOnClickListener {
            // Call the ViewModel method to download or overwrite the CSV file
            downloadCsvFile()
        }
    }

    private fun requestNecessaryPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            for (i in permissions.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "${permissions[i]} permission not granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /*      WORKING FOR FIRST TIME DOWNLOAD
    private fun downloadCsvFile() {
        val file = FileUtils.getCsvFile(this)

        if (file != null) {

            if (!file.exists()) {
                file.createNewFile()
                // Optionally, write some default data to file if necessary
                file.writeText("IP Address, Latitude, Longitude, Local Time, UTC Time\n")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val contentUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)?.also { uri ->
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        file.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }

                if (contentUri != null) {
                    Toast.makeText(this, "CSV file downloaded", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to download file", Toast.LENGTH_SHORT).show()
                }
            } else {
                val destFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), file.name)

                if (destFile.exists()) {
                    destFile.delete() // Delete the existing file
                }

                file.copyTo(destFile)
                Toast.makeText(this, "CSV file downloaded", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
        }
    }*/

    /*private fun downloadCsvFile() {
        val file = FileUtils.getCsvFile(this)

        if (file != null && file.exists()) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                // Query to check if the file already exists in the Downloads folder
                val existingFileUri = contentResolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.MediaColumns._ID),
                    "${MediaStore.MediaColumns.DISPLAY_NAME}=?",
                    arrayOf(file.name),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                        Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
                    } else null
                }

                existingFileUri?.let { uri ->
                    // If the file exists, delete it first
                    contentResolver.delete(uri, null, null)
                }

                val contentUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)?.also { uri ->
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        file.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }

                if (contentUri != null) {
                    Toast.makeText(this, "CSV file downloaded", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to download file", Toast.LENGTH_SHORT).show()
                }
            } else {
                val destFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), file.name)

                if (destFile.exists()) {
                    destFile.delete() // Delete the existing file
                }

                file.copyTo(destFile)
                Toast.makeText(this, "CSV file downloaded", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
        }
    }*/

    private fun downloadCsvFile() {
        val file = FileUtils.getCsvFile(this)

        if (file != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android Q (API 29) and above
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                // Check if the file already exists in MediaStore
                val queryUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val projection = arrayOf(MediaStore.MediaColumns._ID)
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                val selectionArgs = arrayOf(file.name)

                val cursor = contentResolver.query(queryUri, projection, selection, selectionArgs, null)
                val existingId = cursor?.use {
                    if (it.moveToFirst()) {
                        it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    } else {
                        null
                    }
                }

                if (existingId != null) {
                    // File already exists, so we need to update it
                    val updateUri = ContentUris.withAppendedId(queryUri, existingId)
                    contentResolver.openOutputStream(updateUri)?.use { outputStream ->
                        file.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    Toast.makeText(this, "CSV file updated", Toast.LENGTH_SHORT).show()
                } else {
                    // File does not exist, so we need to insert it
                    val contentUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)?.also { uri ->
                        contentResolver.openOutputStream(uri)?.use { outputStream ->
                            file.inputStream().use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    }

                    if (contentUri != null) {
                        Toast.makeText(this, "CSV file downloaded", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to download file", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // For Android versions below Q
                val destFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), file.name)

                if (destFile.exists()) {
                    destFile.delete() // Delete the existing file
                }

                // Copy the file to the Downloads directory
                file.copyTo(destFile)
                Toast.makeText(this, "CSV file downloaded", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
        }
    }
}

