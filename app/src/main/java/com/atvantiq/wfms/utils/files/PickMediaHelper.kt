package com.atvantiq.wfms.utils.files

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.atvantiq.wfms.BuildConfig
import com.atvantiq.wfms.R
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.UUID
import kotlin.math.sqrt

class PickMediaHelper(
    private val context: Context,
    private val cameraLauncher: ActivityResultLauncher<Uri>,
    private val galleryLauncher: ActivityResultLauncher<Intent>,
    private val permissionLauncher: ActivityResultLauncher<Array<String>>,
    private val callback: Callback
) {
    private var photoFile: File? = null
    private val fileExtensions = setOf("jpg", "png", "jpeg", "webp")
    private var actionId = 0

    // Optional: set from Activity/Fragment (recommended) for best behavior.
    private var photoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>? = null

    fun setPhotoPickerLauncher(launcher: ActivityResultLauncher<PickVisualMediaRequest>?) {
        photoPickerLauncher = launcher
    }

    fun setActionId(id: Int) {
        actionId = id
    }

    fun showDialog() {
        AlertDialog.Builder(context, R.style.CustomAlertDialog)
            .setTitle(R.string.pick_image)
            .setItems(R.array.source_items) { _, which ->
                when (which) {
                    0 -> requestCameraPermission()
                    1 -> openGallery()
                }
            }.show()
    }

    fun onlyCameraMedia() {
        requestCameraPermission()
    }

    private fun requestCameraPermission() {
        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
    }

    /**
     * Policy-friendly: do NOT request READ_MEDIA_IMAGES/READ_EXTERNAL_STORAGE.
     * Use Photo Picker when available; otherwise fallback to ACTION_PICK (single item).
     */
    private fun openGallery() {
        val picker = photoPickerLauncher
        if (picker != null && ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            return
        }
        launchLegacyGallery()
    }

    fun handlePermissionResult(grantedPermissions: Map<String, Boolean>) {
        // Only CAMERA permission is handled here now.
        if (grantedPermissions[Manifest.permission.CAMERA] == true) {
            launchCamera()
        } else if (grantedPermissions.containsKey(Manifest.permission.CAMERA)) {
            callback.onError(context.getString(R.string.camera_permission_msg))
        }
        // If something else comes through, ignore (no gallery permissions requested anymore).
    }

    private fun launchCamera() {
        photoFile = createFile()
        photoFile?.let {
            val authority = "${BuildConfig.APPLICATION_ID}.provider"
            val uri = FileProvider.getUriForFile(context, authority, it)
            cameraLauncher.launch(uri)
        } ?: callback.onError(context.getString(R.string.file_creation_error))
    }

    private fun launchLegacyGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(galleryIntent)
    }

    fun handlePhotoPickerResult(uri: Uri?) {
        if (uri == null) {
            callback.onError(context.getString(R.string.gallery_error))
            return
        }
        handlePickedUri(uri)
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
        if (uri == null) {
            callback.onError(context.getString(R.string.gallery_error))
            return
        }
        handlePickedUri(uri)
    }

    private fun handlePickedUri(uri: Uri) {
        val path = when (uri.scheme?.lowercase(Locale.US)) {
            "content" -> copyUriToCache(uri)
            "file" -> uri.path
            else -> null
        }

        if (path.isNullOrBlank()) {
            callback.onError(context.getString(R.string.gallery_error))
            return
        }

        val file = File(path)
        if (!file.exists()) {
            callback.onError(context.getString(R.string.gallery_error))
            return
        }

        if (!isCorrectFileSize(file)) {
            callback.onError(context.getString(R.string.image_size_error))
            return
        }

        callback.onImagePicked(path, actionId)
    }

    private fun copyUriToCache(uri: Uri): String? {
        return try {
            val ext = guessExtension(uri)
            val outFile = File(context.cacheDir, "picked_${UUID.randomUUID()}.$ext")

            context.contentResolver.openInputStream(uri)?.use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            outFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy picked Uri to cache", e)
            null
        }
    }

    private fun guessExtension(uri: Uri): String {
        val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()
        val fromMime = when (mime) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> null
        }
        if (fromMime != null) return fromMime

        val segment = uri.lastPathSegment ?: return "jpg"
        val dot = segment.lastIndexOf('.')
        val ext = if (dot != -1 && dot < segment.length - 1) segment.substring(dot + 1) else null
        val normalized = ext?.lowercase(Locale.US)
        return if (normalized != null && normalized in fileExtensions) normalized else "jpg"
    }

    private fun isCorrectFileSize(file: File): Boolean {
        val fileSizeMb = file.length() / (1024.0 * 1024.0)
        return fileSizeMb <= 2.0
    }

    private fun createFile(): File? {
        return try {
            FileUtils.createImageFile(context)
        } catch (e: IOException) {
            Log.e(TAG, "File creation failed", e)
            null
        }
    }

    fun decodeBitmap(path: String): Bitmap? {
        return runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
    }

    /**
     * Compress image to <= 1MB and return the path to the compressed file.
     * Returns null if decode/compress fails.
     */
    fun compressImageTo1MB(originalPath: String): String? {
        val bitmap = BitmapFactory.decodeFile(originalPath) ?: return null
        var quality = 90
        val maxSizeBytes = 1024 * 1024 // 1MB

        val compressedFile = try {
            File.createTempFile("compressed_", ".jpg", context.cacheDir)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to create temp file for compression", e)
            bitmap.recycle()
            return null
        }

        fun writeJpeg(bmp: Bitmap, q: Int): Long {
            compressedFile.outputStream().use { os ->
                bmp.compress(Bitmap.CompressFormat.JPEG, q, os)
                os.flush()
            }
            return compressedFile.length()
        }

        // Pass 1: lower quality
        while (quality > 10) {
            val size = writeJpeg(bitmap, quality)
            if (size <= maxSizeBytes) break
            quality -= 10
        }

        // Pass 2: resize if still too large
        if (compressedFile.length() > maxSizeBytes) {
            val current = compressedFile.length().toDouble().coerceAtLeast(1.0)
            val scale = sqrt(maxSizeBytes.toDouble() / current).coerceIn(0.1, 1.0)
            val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)

            val resized = try {
                Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resize bitmap", e)
                bitmap.recycle()
                return null
            }

            writeJpeg(resized, quality.coerceAtLeast(20))
            if (resized !== bitmap) resized.recycle()
        }

        bitmap.recycle()
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
