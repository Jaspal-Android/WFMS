package com.atvantiq.wfms.utils.files

import android.Manifest
import timber.log.Timber
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val imageProcessor = ImageProcessor(context)

    // Files this helper created itself (camera captures, copies of gallery picks). They are
    // redundant once the upload-sized JPEG exists, so they are deleted then. A path a caller
    // handed us (a legacy file:// pick) is never in here and is never deleted.
    private val ownedFiles = mutableSetOf<String>()

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
        photoFile?.let { ownedFiles += it.absolutePath }
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
            "file" -> uri.path?.takeUnless { PrivateFiles.isInside(File(context.applicationInfo.dataDir), File(it)) }
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
            callback.onError(context.getString(R.string.image_size_error, MAX_SOURCE_MB))
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

            ownedFiles += outFile.absolutePath
            outFile.absolutePath
        } catch (e: Exception) {
            Timber.e(e, "Failed to copy picked Uri to cache")
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

    // Only a sanity limit: the photo is downsized and re-encoded to <= 1 MB before upload, so a
    // normal 3-8 MB phone photo must be accepted. (It used to be rejected above 2 MB, while
    // camera photos of the same size were fine.)
    private fun isCorrectFileSize(file: File): Boolean = file.length() <= MAX_SOURCE_BYTES

    private fun createFile(): File? {
        return try {
            FileUtils.createImageFile(context)
        } catch (e: IOException) {
            Timber.e(e, "File creation failed")
            null
        }
    }

    /**
     * Decodes, orients and compresses [path] on a background thread, then calls [onReady] on the
     * main thread with the file to upload and a preview. If the photo cannot be processed the
     * callback's [Callback.onError] is called instead. [onReady] is not called once [scope] ends.
     */
    fun prepareImage(path: String, scope: CoroutineScope, onReady: (PreparedImage) -> Unit) {
        scope.launch {
            val prepared = withContext(Dispatchers.Default) {
                imageProcessor.prepare(path)?.also { discardIfOwned(path) }
            }
            if (prepared != null) {
                onReady(prepared)
            } else {
                callback.onError(context.getString(R.string.image_process_error))
            }
        }
    }

    private fun discardIfOwned(path: String) {
        if (ownedFiles.remove(path)) File(path).delete()
    }

    /**
     * A small, upright bitmap to preview [path] with. Cheap enough for a screen that does not
     * upload the photo; screens that do should use [prepareImage].
     */
    fun decodeBitmap(path: String): Bitmap? = imageProcessor.decodePreview(path)

    interface Callback {
        fun onImagePicked(path: String, request: Int)
        fun onError(message: String)
    }

    companion object {
        private const val MAX_SOURCE_MB = 25
        private const val MAX_SOURCE_BYTES = MAX_SOURCE_MB * 1024L * 1024L
    }
}
