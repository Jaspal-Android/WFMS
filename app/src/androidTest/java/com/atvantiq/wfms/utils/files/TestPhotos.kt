package com.atvantiq.wfms.utils.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.ExifInterface
import java.io.File
import java.util.Random

/** Generates real JPEG files for image tests. */
object TestPhotos {

    /** A JPEG full of colored noise, so it is genuinely large (several MB at 4000x3000). */
    fun write(context: Context, width: Int, height: Int, orientation: Int? = null): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val random = Random(7)
        canvas.drawColor(Color.WHITE)
        repeat(30000) {
            paint.color = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
            val x = random.nextInt(width - 40)
            val y = random.nextInt(height - 40)
            canvas.drawRect(
                x.toFloat(), y.toFloat(),
                (x + 8 + random.nextInt(32)).toFloat(), (y + 8 + random.nextInt(32)).toFloat(), paint
            )
        }
        val file = File.createTempFile("test_photo_", ".jpg", context.cacheDir)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
        if (orientation != null) {
            ExifInterface(file.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                saveAttributes()
            }
        }
        return file
    }
}
