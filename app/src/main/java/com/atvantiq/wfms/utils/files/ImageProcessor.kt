package com.atvantiq.wfms.utils.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Log
import androidx.core.graphics.scale
import java.io.File
import java.io.IOException

/** A photo ready to use: the file to upload, and a small bitmap for the on-screen preview. */
class PreparedImage(val uploadPath: String, val preview: Bitmap)

/**
 * Turns a picked or captured photo into an upload-sized JPEG and a preview.
 *
 * The previous code decoded the full-resolution bitmap on the main thread (twice: once to
 * compress, once for the preview), ignored the EXIF orientation, and re-encoded up to nine times.
 * A 48 MP photo needs ~190 MB as ARGB_8888, so that meant an OutOfMemoryError or an ANR, and
 * portrait photos were uploaded sideways.
 *
 * This decodes subsampled, applies the orientation, and encodes once. [prepare] blocks: call it
 * off the main thread.
 */
class ImageProcessor(private val context: Context) {

    /** Returns null when the file cannot be decoded or cannot be brought under the size limit. */
    fun prepare(sourcePath: String): PreparedImage? {
        val (width, height) = readBounds(sourcePath) ?: return null
        // Sample towards 75% of the upload size, so a 4000 px photo decodes at 2000 px (12 MB)
        // instead of at 4000 px (48 MB) only to be scaled down straight afterwards.
        var sample = ImageSizing.sampleSize(width, height, UPLOAD_MAX_SIDE * 3 / 4)
        repeat(OOM_RETRIES) {
            try {
                return decodeAndEncode(sourcePath, sample)
            } catch (e: OutOfMemoryError) {
                Log.w(TAG, "Out of memory at sampleSize=$sample, retrying smaller", e)
                sample *= 2
            }
        }
        return null
    }

    /** A bitmap small enough to show in an ImageView, upright. Null if it cannot be decoded. */
    fun decodePreview(sourcePath: String, maxSide: Int = PREVIEW_MAX_SIDE): Bitmap? = try {
        val (width, height) = readBounds(sourcePath) ?: return null
        val decoded = BitmapFactory.decodeFile(
            sourcePath,
            BitmapFactory.Options().apply { inSampleSize = ImageSizing.sampleSize(width, height, maxSide) }
        ) ?: return null
        val upright = applyOrientation(decoded, readOrientation(sourcePath))
        scaleToFit(upright, maxSide)
    } catch (e: OutOfMemoryError) {
        Log.w(TAG, "Out of memory decoding preview", e)
        null
    }

    private fun decodeAndEncode(sourcePath: String, sample: Int): PreparedImage? {
        val decoded = BitmapFactory.decodeFile(
            sourcePath,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: return null
        val upright = applyOrientation(decoded, readOrientation(sourcePath))
        val fitted = scaleToFit(upright, UPLOAD_MAX_SIDE)

        val output = try {
            File.createTempFile("compressed_", ".jpg", context.cacheDir)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to create temp file for compression", e)
            fitted.recycle()
            return null
        }

        val preview = scaleToFit(fitted, PREVIEW_MAX_SIDE, copy = true)
        return if (encodeUnderLimit(fitted, output)) {
            PreparedImage(output.absolutePath, preview)
        } else {
            output.delete()
            preview.recycle()
            null
        }
    }

    /**
     * Writes [source] as a JPEG under [MAX_UPLOAD_BYTES]: lower the quality first, then shrink the
     * image, so a photo is sized once rather than encoded at full resolution repeatedly.
     */
    private fun encodeUnderLimit(source: Bitmap, output: File): Boolean {
        var work = source
        var quality = START_QUALITY
        try {
            repeat(MAX_ATTEMPTS) {
                output.outputStream().use { work.compress(Bitmap.CompressFormat.JPEG, quality, it) }
                if (output.length() <= MAX_UPLOAD_BYTES) return true
                if (quality > MIN_QUALITY) {
                    quality -= QUALITY_STEP
                } else {
                    val (w, h) = ImageSizing.fitWithin(
                        work.width, work.height, (maxOf(work.width, work.height) * SHRINK_FACTOR).toInt()
                    )
                    if (maxOf(w, h) < MIN_SIDE) return false
                    val smaller = work.scale(w, h)
                    if (work !== source) work.recycle()
                    work = smaller
                    quality = QUALITY_AFTER_SHRINK
                }
            }
            return false
        } finally {
            if (work !== source) work.recycle()
            source.recycle()
        }
    }

    private fun readBounds(path: String): Pair<Int, Int>? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        return if (bounds.outWidth > 0 && bounds.outHeight > 0) bounds.outWidth to bounds.outHeight else null
    }

    private fun readOrientation(path: String): Int = try {
        ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (e: IOException) {
        ExifInterface.ORIENTATION_NORMAL
    }

    /** Bakes the EXIF orientation into the pixels. Recycles [bitmap] if a new one is created. */
    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val transform = ImageSizing.exifTransform(orientation)
        if (transform.isIdentity) return bitmap
        val matrix = Matrix().apply {
            postRotate(transform.rotationDegrees.toFloat())
            if (transform.mirrored) postScale(-1f, 1f)
        }
        val upright = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (upright !== bitmap) bitmap.recycle()
        return upright
    }

    /**
     * Scales [bitmap] down to fit in [maxSide]. With [copy] the result is always a new bitmap, so
     * the caller may recycle the original independently.
     */
    private fun scaleToFit(bitmap: Bitmap, maxSide: Int, copy: Boolean = false): Bitmap {
        val (w, h) = ImageSizing.fitWithin(bitmap.width, bitmap.height, maxSide)
        if (w == bitmap.width && h == bitmap.height) {
            return if (copy) bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false) else bitmap
        }
        val scaled = bitmap.scale(w, h)
        if (!copy && scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    companion object {
        private const val TAG = "ImageProcessor"

        /** Longest side of the uploaded photo. Plenty for a legible receipt or site photo. */
        const val UPLOAD_MAX_SIDE = 2048

        /** Longest side of the on-screen preview. */
        const val PREVIEW_MAX_SIDE = 1024

        const val MAX_UPLOAD_BYTES = 1024 * 1024L

        private const val START_QUALITY = 85
        private const val MIN_QUALITY = 45
        private const val QUALITY_STEP = 10
        private const val QUALITY_AFTER_SHRINK = 70
        private const val SHRINK_FACTOR = 0.8
        private const val MIN_SIDE = 400
        private const val MAX_ATTEMPTS = 12
        private const val OOM_RETRIES = 2
    }
}
