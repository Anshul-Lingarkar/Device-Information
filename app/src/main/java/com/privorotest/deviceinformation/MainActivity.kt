package com.privorotest.deviceinformation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.privorotest.deviceinformation.utils.FileUtils

class MainActivity : AppCompatActivity(), BaseApplicationContract.ViewContract {

    private lateinit var ipTextView: TextView
    private lateinit var locationTextView: TextView
    private lateinit var datetimeTextView: TextView
    private lateinit var downloadButton: Button
    private val REQUEST_CODE_PERMISSIONS = 101
    private val FILE_TYPE = "text/csv"
    private val networkViewModel: NetworkViewModel by viewModels()
    private var permissionsGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestNecessaryPermissions()

        // Initialize UI elements
        ipTextView = findViewById(R.id.ip_text_view)
        locationTextView = findViewById(R.id.location_text_view)
        datetimeTextView = findViewById(R.id.datetime_text_view)
        downloadButton = findViewById(R.id.download_button)

        // If permissions have been granted, setup the UI
        if (permissionsGranted) {
            setupUI()
        }
        downloadButton.setOnClickListener {
            val csvFile = FileUtils.getCsvFile(this)
            if (csvFile == null || !csvFile.exists() || csvFile.length() == 0L) {
                Toast.makeText(
                    this,
                    getString(R.string.csv_file_generation_error),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val csvFileUri = FileUtils.getCsvFileUri(this)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = FILE_TYPE
                putExtra(Intent.EXTRA_STREAM, csvFileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                startActivity(
                    Intent.createChooser(
                        shareIntent,
                        getString(R.string.csv_file_share_screen_title)
                    )
                )
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.csv_file_share_error), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun setupUI() {
        networkViewModel.networkData.observe(this, Observer { networkData ->
            ipTextView.text = networkData.ipAddress ?: getString(R.string.ip_default)
            locationTextView.text = networkData.location?.let {
                getString(R.string.location_format, it.latitude, it.longitude)
            } ?: getString(R.string.location_default)
            datetimeTextView.text =
                getString(R.string.datetime_format, networkData.localTime, networkData.utcTime)
        })
    }

    private fun showPermissionRequiredMessage() {
        val rootView = findViewById<View>(android.R.id.content)
        Snackbar.make(
            rootView,
            getString(R.string.permission_required_message),
            Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            permissionsGranted = grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (permissionsGranted) {
                // Setup UI after permissions have been granted
                setupUI()
            } else {
                showPermissionRequiredMessage()
            }
        }
    }

    private fun requestNecessaryPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                REQUEST_CODE_PERMISSIONS
            )
        } else {
            permissionsGranted = true
            setupUI()
        }
    }
}
