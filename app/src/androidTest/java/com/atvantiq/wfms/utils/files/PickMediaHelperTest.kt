package com.atvantiq.wfms.utils.files

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.atvantiq.wfms.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** The picker end to end on a device: a gallery pick becomes one small upright JPEG. */
@RunWith(AndroidJUnit4::class)
class PickMediaHelperTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val created = mutableListOf<File>()

    @After
    fun tearDown() {
        created.forEach { it.delete() }
    }

    private fun <I> dummyLauncher() = object : ActivityResultLauncher<I>() {
        override fun launch(input: I, options: ActivityOptionsCompat?) = Unit
        override fun unregister() = Unit
        override val contract: ActivityResultContract<I, *> get() = throw UnsupportedOperationException()
    }

    private class Outcome {
        val done = CountDownLatch(1)
        var pickedPath: String? = null
        var prepared: PreparedImage? = null
        var error: String? = null
    }

    /** Picks [source] the way the Photo Picker hands it over (a content Uri) and waits for the result. */
    private fun pick(source: File): Outcome {
        val outcome = Outcome()
        lateinit var helper: PickMediaHelper
        helper = PickMediaHelper(
            context, dummyLauncher<android.net.Uri>(), dummyLauncher<Intent>(), dummyLauncher<Array<String>>(),
            object : PickMediaHelper.Callback {
                override fun onImagePicked(path: String, request: Int) {
                    outcome.pickedPath = path
                    helper.prepareImage(path, scope) {
                        outcome.prepared = it
                        outcome.done.countDown()
                    }
                }

                override fun onError(message: String) {
                    outcome.error = message
                    outcome.done.countDown()
                }
            }
        )
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", source)
        helper.handlePhotoPickerResult(uri)
        assertTrue("timed out waiting for the image", outcome.done.await(30, TimeUnit.SECONDS))
        outcome.prepared?.let { created += File(it.uploadPath) }
        return outcome
    }

    @Test
    fun aGalleryPickEndsUpAsOneSmallJpegAndTheTemporaryCopyIsDeleted() {
        val source = TestPhotos.write(context, 4000, 3000).also { created += it }

        val outcome = pick(source)

        assertNull(outcome.error)
        val prepared = assertNotNull(outcome.prepared).let { outcome.prepared!! }
        val upload = File(prepared.uploadPath)
        assertTrue("upload is ${upload.length()} bytes", upload.length() in 1..ImageProcessor.MAX_UPLOAD_BYTES)
        assertTrue(maxOf(prepared.preview.width, prepared.preview.height) <= ImageProcessor.PREVIEW_MAX_SIDE)

        val picked = File(outcome.pickedPath!!)
        assertTrue("the copy the helper made should be named picked_*", picked.name.startsWith("picked_"))
        assertFalse("the temporary copy must be deleted once the upload file exists", picked.exists())
        assertTrue("the caller's own file must never be deleted", source.exists())
    }

    @Test
    fun aSidewaysPhotoComesOutUpright() {
        val source = TestPhotos.write(context, 4000, 3000, android.media.ExifInterface.ORIENTATION_ROTATE_90)
            .also { created += it }

        val outcome = pick(source)

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(outcome.prepared!!.uploadPath, options)
        assertTrue("expected portrait, got ${options.outWidth}x${options.outHeight}", options.outHeight > options.outWidth)
    }

    @Test
    fun anImageThatCannotBeDecodedReportsAnErrorAndCleansUp() {
        val junk = File.createTempFile("junk_", ".jpg", context.cacheDir).also {
            created += it
            it.writeBytes(ByteArray(4096) { i -> (i * 17).toByte() })
        }

        val outcome = pick(junk)

        assertNull(outcome.prepared)
        assertEquals(context.getString(com.atvantiq.wfms.R.string.image_process_error), outcome.error)
    }
}
