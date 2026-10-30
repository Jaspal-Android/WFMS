package com.atvantiq.wfms.utils.files

import android.graphics.BitmapFactory
import android.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Runs the real bitmap pipeline on a device. The JVM cannot: android.graphics needs a runtime.
 * `adb shell am instrument -w -e class com.atvantiq.wfms.utils.files.ImageProcessorTest <app id>.test/androidx.test.runner.AndroidJUnitRunner`
 */
@RunWith(AndroidJUnit4::class)
class ImageProcessorTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var processor: ImageProcessor
    private val created = mutableListOf<File>()

    @Before
    fun setUp() {
        processor = ImageProcessor(context)
    }

    @After
    fun tearDown() {
        created.forEach { it.delete() }
    }

    private fun writePhoto(width: Int, height: Int, orientation: Int? = null): File =
        TestPhotos.write(context, width, height, orientation).also { created += it }

    private fun bounds(path: String): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        return options.outWidth to options.outHeight
    }

    private fun prepared(source: File): PreparedImage {
        val result = processor.prepare(source.absolutePath)
        assertNotNull("prepare() returned null for ${source.name}", result)
        created += File(result!!.uploadPath)
        return result
    }

    @Test
    fun aLargePhotoIsBroughtUnderOneMegabyteAndTheUploadSize() {
        val source = writePhoto(4000, 3000)
        assertTrue("the test photo must start out well over the limit, was ${source.length()} bytes", source.length() > 2 * ImageProcessor.MAX_UPLOAD_BYTES)

        val result = prepared(source)

        val upload = File(result.uploadPath)
        assertTrue("upload is ${upload.length()} bytes", upload.length() <= ImageProcessor.MAX_UPLOAD_BYTES)
        val (w, h) = bounds(result.uploadPath)
        assertTrue("upload is ${w}x$h", maxOf(w, h) <= ImageProcessor.UPLOAD_MAX_SIDE)
        assertTrue("upload keeps the landscape aspect ratio", w > h)
        assertEquals("aspect ratio preserved", 4.0 / 3.0, w.toDouble() / h, 0.02)
    }

    @Test
    fun thePreviewIsSmallAndKeepsTheSameAspectRatio() {
        val result = prepared(writePhoto(4000, 3000))

        assertTrue(maxOf(result.preview.width, result.preview.height) <= ImageProcessor.PREVIEW_MAX_SIDE)
        assertEquals(4.0 / 3.0, result.preview.width.toDouble() / result.preview.height, 0.02)
    }

    @Test
    fun aPhotoStoredSidewaysIsUploadedUpright() {
        // A portrait shot on most phones is a landscape file tagged ORIENTATION_ROTATE_90.
        val result = prepared(writePhoto(4000, 3000, ExifInterface.ORIENTATION_ROTATE_90))

        val (w, h) = bounds(result.uploadPath)
        assertTrue("expected portrait after rotation, got ${w}x$h", h > w)
        assertTrue("preview is upright too", result.preview.height > result.preview.width)
    }

    @Test
    fun aPhotoWithoutARotationTagStaysLandscape() {
        val (w, h) = bounds(prepared(writePhoto(4000, 3000, ExifInterface.ORIENTATION_NORMAL)).uploadPath)

        assertTrue("got ${w}x$h", w > h)
    }

    @Test
    fun aSmallPhotoIsNotUpscaled() {
        val (w, h) = bounds(prepared(writePhoto(800, 600)).uploadPath)

        assertEquals(800 to 600, w to h)
    }

    @Test
    fun aCorruptFileIsRejectedInsteadOfCrashing() {
        val junk = File.createTempFile("junk_", ".jpg", context.cacheDir).also {
            created += it
            it.writeBytes(ByteArray(2048) { i -> (i * 31).toByte() })
        }

        assertNull(processor.prepare(junk.absolutePath))
    }

    @Test
    fun aMissingFileIsRejectedInsteadOfCrashing() {
        assertNull(processor.prepare(File(context.cacheDir, "does_not_exist.jpg").absolutePath))
    }

    @Test
    fun decodePreviewIsSmallAndUpright() {
        val bitmap = processor.decodePreview(writePhoto(4000, 3000, ExifInterface.ORIENTATION_ROTATE_90).absolutePath)

        assertNotNull(bitmap)
        assertTrue(maxOf(bitmap!!.width, bitmap.height) <= ImageProcessor.PREVIEW_MAX_SIDE)
        assertTrue("upright: ${bitmap.width}x${bitmap.height}", bitmap.height > bitmap.width)
    }
}
