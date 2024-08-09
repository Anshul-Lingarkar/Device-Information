package com.privorotest.deviceinformation

import android.Manifest
import android.app.DownloadManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
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
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import com.privorotest.deviceinformation.utils.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

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
            /*val csvFileUri = FileUtils.getCsvFileUri(this)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, csvFileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Explicitly grant URI permissions to all relevant apps
            val resolvedIntentActivities = packageManager.queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY)
            resolvedIntentActivities.forEach { resolvedInfo ->
                val packageName = resolvedInfo.activityInfo.packageName
                grantUriPermission(packageName, csvFileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            try {
                startActivity(Intent.createChooser(shareIntent, "Share CSV File"))
            } catch (e: Exception) {
                Toast.makeText(this, "No app available to share the file.", Toast.LENGTH_SHORT).show()
            }*/
            val csvFile = FileUtils.getCsvFile(this)
            if (csvFile == null || !csvFile.exists() || csvFile.length() == 0L) {
                Toast.makeText(this, "CSV file is empty or doesn't exist.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val csvFileUri = FileUtils.getCsvFileUri(this)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, csvFileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            try {
                startActivity(Intent.createChooser(shareIntent, "Share CSV File"))
            } catch (e: Exception) {
                Toast.makeText(this, "No app available to share the file.", Toast.LENGTH_SHORT).show()
            }
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
}

