package com.atvantiq.wfms.perf

import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.atvantiq.wfms.R
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The splash and new-password screens show the logo in a 176dp card. It sits on the cold-start
 * path, so it must not decode to tens of megabytes. Run on a device or emulator:
 * `adb shell am instrument -w -e class com.atvantiq.wfms.perf.LogoMemoryTest <app id>.test/androidx.test.runner.AndroidJUnitRunner`
 */
@RunWith(AndroidJUnit4::class)
class LogoMemoryTest {

    @Test
    fun logoDecodesToAFewMegabytesAtMost() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val startedAt = System.nanoTime()
        val bitmap = (ContextCompat.getDrawable(context, R.drawable.ic_logo) as BitmapDrawable).bitmap
        val decodeMs = (System.nanoTime() - startedAt) / 1_000_000.0
        assertNotNull(bitmap)

        Log.i(
            "LogoMemoryTest",
            "ic_logo decoded to ${bitmap.width}x${bitmap.height} = ${bitmap.allocationByteCount} bytes in %.1f ms".format(decodeMs)
        )

        assertTrue(
            "ic_logo decodes to %.1f MB (limit 4 MB)".format(bitmap.allocationByteCount / 1024.0 / 1024.0),
            bitmap.allocationByteCount <= 4 * 1024 * 1024
        )
    }
}
