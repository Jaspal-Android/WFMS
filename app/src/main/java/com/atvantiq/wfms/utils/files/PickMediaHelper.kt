package com.atvantiq.wfms.utils.files

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.atvantiq.wfms.BuildConfig
import com.atvantiq.wfms.R
import java.io.File
import java.io.IOException
import java.util.*

class PickMediaHelper(
    private val context: Context,
    private val cameraLauncher: ActivityResultLauncher<Uri>,
    private val galleryLauncher: ActivityResultLauncher<Intent>,
    private val permissionLauncher: ActivityResultLauncher<Array<String>>,
    private val callback: Callback
) {

    private val READ_IMAGES_PERMISSION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    private var photoFile: File? = null
    private val fileExtensions = arrayOf("jpg", "png", "jpeg")
    private var actionId = 0

    fun setActionId(id: Int) {
        actionId = id
    }

    fun showDialog() {
        AlertDialog.Builder(context, R.style.CustomAlertDialog)
            .setTitle(R.string.pick_image)
            .setItems(R.array.source_items) { _, which ->
                when (which) {
                    0 -> requestCameraPermission()
                    1 -> requestGalleryPermission()
                }
            }.show()
    }

    fun onlyCameraMedia(){
      requestCameraPermission()
    }

    private fun requestCameraPermission() {
        permissionLauncher.launch(arrayOf(android.Manifest.permission.CAMERA))
    }

    private fun requestGalleryPermission() {
        permissionLauncher.launch(arrayOf(READ_IMAGES_PERMISSION))
    }

    fun handlePermissionResult(grantedPermissions: Map<String, Boolean>) {
        if (grantedPermissions[android.Manifest.permission.CAMERA] == true) {
            launchCamera()
        } else if (grantedPermissions[READ_IMAGES_PERMISSION] == true) {
            launchGallery()
        } else {
            callback.onError(context.getString(R.string.camera_permission_msg))
        }
    }

    private fun launchCamera() {
        photoFile = createFile()
        photoFile?.let {
            val authority = "${BuildConfig.APPLICATION_ID}.provider"
            val uri = FileProvider.getUriForFile(context, authority, it)
            cameraLauncher.launch(uri)
        } ?: callback.onError(context.getString(R.string.file_creation_error))
    }

    private fun launchGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(galleryIntent)
    }

    fun handleCameraResult(success: Boolean) {
        if (success && photoFile != null) {
            callback.onImagePicked(photoFile!!.absolutePath, actionId)
        } else {
            callback.onError(context.getString(R.string.camera_error))
        }
    }

    fun handleGalleryResult(data: Intent?) {
        val uri = data?.data
        if (uri != null) {
            val realPath: String? = FileUtils.getRealPathFromUri(context, data?.data!!)
            if (realPath != null && isCorrectFileSize(File(realPath))) {
                callback.onImagePicked(realPath, actionId)
            } else {
                callback.onError(context.getString(R.string.image_size_error))
            }
        } else {
            callback.onError(context.getString(R.string.gallery_error))
        }
    }

    private fun isCorrectFileSize(file: File): Boolean {
        val fileSize = file.length() / (1024 * 1024)
        return fileSize <= 2
    }

    private fun createFile(): File? {
        return try {
            FileUtils.createImageFile(context)
        } catch (e: IOException) {
            Log.e(TAG, "File creation failed", e)
            null
        }
    }

    fun decodeBitmap(path: String): Bitmap {
        return BitmapFactory.decodeFile(path)
    }

    /**
     * Compress image to <= 1MB and return the path to the compressed file.
     * Supports SDK 24-34.
     */
    fun compressImageTo1MB(originalPath: String): String? {
        val originalFile = File(originalPath)
        val bitmap = BitmapFactory.decodeFile(originalPath)
        var quality = 90
        val maxSizeBytes = 1024 * 1024 // 1MB

        // Create a temp file for compressed image
        val compressedFile = File.createTempFile("compressed_", ".jpg", context.cacheDir)
        var outputStream = compressedFile.outputStream()

        // Compress with decreasing quality until <= 1MB or quality too low
        while (quality > 10) {
            outputStream = compressedFile.outputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.flush()
            outputStream.close()
            if (compressedFile.length() <= maxSizeBytes) break
            quality -= 10
        }

        // If still too large, resize bitmap and try again
        if (compressedFile.length() > maxSizeBytes) {
            val scale = Math.sqrt(maxSizeBytes.toDouble() / compressedFile.length())
            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            outputStream = compressedFile.outputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.flush()
            outputStream.close()
        }

        return if (compressedFile.length() <= maxSizeBytes) compressedFile.absolutePath else null
    }

    interface Callback {
        fun onImagePicked(path: String, request: Int)
        fun onError(message: String)
    }

    companion object {
        const val TAG = "PickMediaHelper"
    }
}
