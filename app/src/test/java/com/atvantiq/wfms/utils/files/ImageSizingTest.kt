package com.atvantiq.wfms.utils.files

import android.media.ExifInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageSizingTest {

    // ---- sampleSize ----

    @Test
    fun `a large photo is decoded subsampled instead of at full resolution`() {
        assertEquals(2, ImageSizing.sampleSize(4000, 3000, 1536))
        assertEquals(4, ImageSizing.sampleSize(8000, 6000, 1536)) // 48 MP: ~190 MB full size
    }

    @Test
    fun `the decoded image never ends up smaller than the target`() {
        listOf(1200 to 900, 3000 to 2250, 4032 to 3024, 6000 to 4000, 9248 to 6936).forEach { (w, h) ->
            val sample = ImageSizing.sampleSize(w, h, 1536)
            assertTrue("$w x $h -> sample $sample", maxOf(w, h) / sample >= minOf(1536, maxOf(w, h)))
            assertEquals("power of two", 0, sample and (sample - 1))
        }
    }

    @Test
    fun `a photo that is already small is not subsampled`() {
        assertEquals(1, ImageSizing.sampleSize(1000, 800, 1536))
        assertEquals(1, ImageSizing.sampleSize(3000, 2250, 1536))
    }

    @Test
    fun `sampling looks at the longest side, so portrait and landscape agree`() {
        assertEquals(ImageSizing.sampleSize(4000, 3000, 1536), ImageSizing.sampleSize(3000, 4000, 1536))
    }

    @Test
    fun `nonsense dimensions fall back to no subsampling`() {
        assertEquals(1, ImageSizing.sampleSize(0, 0, 1536))
        assertEquals(1, ImageSizing.sampleSize(-5, 100, 1536))
        assertEquals(1, ImageSizing.sampleSize(4000, 3000, 0))
    }

    // ---- fitWithin ----

    @Test
    fun `fitWithin scales down keeping the aspect ratio`() {
        assertEquals(2048 to 1536, ImageSizing.fitWithin(4000, 3000, 2048))
        assertEquals(1536 to 2048, ImageSizing.fitWithin(3000, 4000, 2048))
    }

    @Test
    fun `fitWithin never scales up`() {
        assertEquals(800 to 600, ImageSizing.fitWithin(800, 600, 2048))
        assertEquals(2048 to 2048, ImageSizing.fitWithin(2048, 2048, 2048))
    }

    @Test
    fun `fitWithin never produces a zero-sized side`() {
        assertEquals(1 to 100, ImageSizing.fitWithin(1, 10_000, 100))
    }

    // ---- EXIF orientation ----

    @Test
    fun `an upright photo needs no transform`() {
        assertTrue(ImageSizing.exifTransform(ExifInterface.ORIENTATION_NORMAL).isIdentity)
        assertTrue(ImageSizing.exifTransform(ExifInterface.ORIENTATION_UNDEFINED).isIdentity)
        assertTrue(ImageSizing.exifTransform(99).isIdentity)
    }

    @Test
    fun `the common camera orientations rotate the photo upright`() {
        // Portrait shots on most phones are stored landscape with orientation 6 (or 8).
        assertEquals(ExifTransform(90, false), ImageSizing.exifTransform(ExifInterface.ORIENTATION_ROTATE_90))
        assertEquals(ExifTransform(180, false), ImageSizing.exifTransform(ExifInterface.ORIENTATION_ROTATE_180))
        assertEquals(ExifTransform(270, false), ImageSizing.exifTransform(ExifInterface.ORIENTATION_ROTATE_270))
    }

    @Test
    fun `mirrored orientations are rotated and then mirrored`() {
        assertEquals(ExifTransform(0, true), ImageSizing.exifTransform(ExifInterface.ORIENTATION_FLIP_HORIZONTAL))
        assertEquals(ExifTransform(180, true), ImageSizing.exifTransform(ExifInterface.ORIENTATION_FLIP_VERTICAL))
        assertEquals(ExifTransform(90, true), ImageSizing.exifTransform(ExifInterface.ORIENTATION_TRANSPOSE))
        assertEquals(ExifTransform(270, true), ImageSizing.exifTransform(ExifInterface.ORIENTATION_TRANSVERSE))
        assertFalse(ImageSizing.exifTransform(ExifInterface.ORIENTATION_TRANSVERSE).isIdentity)
    }
}
