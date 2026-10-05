package com.atvantiq.wfms.utils.files

import android.content.ContentValues
import timber.log.Timber
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {

    @Throws(IOException::class)
    @JvmStatic
    fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFileName = "WFMS_$timeStamp.jpg"
        val directory = File(context.filesDir, "WFMS")
        if (!directory.exists()) {
            directory.mkdir()
        }
        return File(directory, imageFileName)
    }

    @JvmStatic
    fun createImageFileQ(context: Context): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFileName = "WFMS_$timeStamp.jpg"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/WFMS")
        }

        val resolver = context.contentResolver
        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    @JvmStatic
    fun getRealPathFromUri(context: Context, uri: Uri): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return copyFileToCache(context, uri)
        }
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            if (cursor.moveToFirst()) {
                return cursor.getString(columnIndex)
            }
        }
        return null
    }

    private fun copyFileToCache(context: Context, uri: Uri): String? {
        val cacheFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.jpg")
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(cacheFile).use { outputStream ->
                    val buffer = ByteArray(1024)
                    var length: Int
                    while (inputStream.read(buffer).also { length = it } > 0) {
                        outputStream.write(buffer, 0, length)
                    }
                }
            }
            return cacheFile.absolutePath
        } catch (e: IOException) {
            Timber.e(e, "Error copying file")
        }
        return null
    }
}
