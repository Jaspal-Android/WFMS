package com.atvantiq.wfms.utils.files

/** How to turn a photo's stored pixels upright: rotate clockwise, then optionally mirror. */
data class ExifTransform(val rotationDegrees: Int, val mirrored: Boolean) {
    val isIdentity: Boolean get() = rotationDegrees == 0 && !mirrored
}

/**
 * The arithmetic behind [ImageProcessor], kept free of Android graphics classes so it can be
 * unit tested on the JVM.
 */
object ImageSizing {

    /**
     * The power-of-two `inSampleSize` that decodes the image as small as possible while its
     * longest side stays at least [targetSide]. Decoding at full resolution and scaling down
     * afterwards needs several times more memory (a 48 MP photo is ~190 MB as ARGB_8888).
     */
    fun sampleSize(width: Int, height: Int, targetSide: Int): Int {
        if (width <= 0 || height <= 0 || targetSide <= 0) return 1
        val longest = maxOf(width, height)
        var sample = 1
        while (longest / (sample * 2) >= targetSide) sample *= 2
        return sample
    }

    /** [width] x [height] scaled down to fit inside [maxSide], keeping the aspect ratio. */
    fun fitWithin(width: Int, height: Int, maxSide: Int): Pair<Int, Int> {
        val longest = maxOf(width, height)
        if (longest <= maxSide || longest <= 0) return width to height
        val scale = maxSide.toDouble() / longest
        return maxOf(1, Math.round(width * scale).toInt()) to maxOf(1, Math.round(height * scale).toInt())
    }

    /**
     * Camera photos are usually stored sideways with an EXIF orientation tag. Decoding ignores the
     * tag, and re-encoding to JPEG drops it, so without applying it the upload is sideways.
     */
    fun exifTransform(orientation: Int): ExifTransform = when (orientation) {
        ORIENTATION_FLIP_HORIZONTAL -> ExifTransform(0, true)
        ORIENTATION_ROTATE_180 -> ExifTransform(180, false)
        ORIENTATION_FLIP_VERTICAL -> ExifTransform(180, true)
        ORIENTATION_TRANSPOSE -> ExifTransform(90, true)
        ORIENTATION_ROTATE_90 -> ExifTransform(90, false)
        ORIENTATION_TRANSVERSE -> ExifTransform(270, true)
        ORIENTATION_ROTATE_270 -> ExifTransform(270, false)
        else -> ExifTransform(0, false)
    }

    // EXIF orientation values (EXIF 2.3, tag 0x0112); 1 = already upright.
    private const val ORIENTATION_FLIP_HORIZONTAL = 2
    private const val ORIENTATION_ROTATE_180 = 3
    private const val ORIENTATION_FLIP_VERTICAL = 4
    private const val ORIENTATION_TRANSPOSE = 5
    private const val ORIENTATION_ROTATE_90 = 6
    private const val ORIENTATION_TRANSVERSE = 7
    private const val ORIENTATION_ROTATE_270 = 8
}
