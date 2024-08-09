package com.privorotest.deviceinformation.utils

import android.content.Context
import android.net.Uri
import java.io.File

interface FileUtilsContract {
    fun writeDataToCsv(context: Context, data: String, overwrite: Boolean = false)
    fun getCsvFileUri(context: Context): Uri
    fun getCsvFile(context: Context): File?
}