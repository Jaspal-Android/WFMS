package com.atvantiq.wfms.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.atvantiq.wfms.R
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

/** Round map pins: a filled circle with a ring and a white icon, sized by `map_pin_size`. */
object MapMarkerIcons {

    /** Share of the pin's size left around the icon on each side. */
    private const val ICON_INSET_RATIO = 0.25f

    fun circle(
        context: Context,
        @DrawableRes iconRes: Int,
        @ColorInt fillColor: Int,
        @ColorInt ringColor: Int
    ): BitmapDescriptor {
        val size = context.resources.getDimensionPixelSize(R.dimen.map_pin_size)
        val ring = context.resources.getDimension(R.dimen.map_pin_ring)
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val center = size / 2f
        paint.color = ringColor
        canvas.drawCircle(center, center, center, paint)
        paint.color = fillColor
        canvas.drawCircle(center, center, center - ring, paint)
        ContextCompat.getDrawable(context, iconRes)?.mutate()?.let { icon ->
            icon.setTint(ContextCompat.getColor(context, R.color.white))
            val inset = (size * ICON_INSET_RATIO).toInt()
            icon.setBounds(inset, inset, size - inset, size - inset)
            icon.draw(canvas)
        }
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
