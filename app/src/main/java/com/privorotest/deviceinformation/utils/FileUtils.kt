package com.privorotest.deviceinformation.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.privorotest.deviceinformation.NetworkViewModel.Companion.CSV_FILE_NAME
import java.io.File
import java.io.FileWriter
import java.io.IOException

object FileUtils : FileUtilsContract {
    override fun writeDataToCsv(context: Context, data: String, overwrite: Boolean) {
        val fileName = CSV_FILE_NAME
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            val fileWriter = FileWriter(file, !overwrite)
            fileWriter.append(data)
            fileWriter.append("\n")
            fileWriter.flush()
            fileWriter.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun getCsvFileUri(context: Context): Uri {
        val fileName = CSV_FILE_NAME
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    // New method to get the CSV file
    override fun getCsvFile(context: Context): File? {
        val fileName = CSV_FILE_NAME
        return File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
    }
}
