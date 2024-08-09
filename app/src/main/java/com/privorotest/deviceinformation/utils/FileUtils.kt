package com.privorotest.deviceinformation.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter
import java.io.IOException

object FileUtils {
    fun writeDataToCsv(context: Context, data: String, overwrite: Boolean = false) {
        val fileName = "network_data.csv"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            val fileWriter = FileWriter(file, !overwrite) // Append mode if not overwriting
            if (overwrite || !file.exists()) {
                // Write the header if overwriting or if the file is new
                fileWriter.write("IP Address, Latitude, Longitude, Local Time, UTC Time\n")
            }
            fileWriter.append(data)
            fileWriter.append("\n")
            fileWriter.flush()
            fileWriter.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getCsvFileUri(context: Context): Uri {
        val fileName = "network_data.csv"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    // New method to get the CSV file
    fun getCsvFile(context: Context): File? {
        val fileName = "network_data.csv"
        return File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
    }
}
