package com.atvantiq.wfms.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import kotlin.math.max

class FlowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)

        var x = paddingLeft
        var y = paddingTop
        var rowHeight = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue

            val lp = child.layoutParams as MarginLayoutParams
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)

            val childW = child.measuredWidth + lp.leftMargin + lp.rightMargin
            val childH = child.measuredHeight + lp.topMargin + lp.bottomMargin

            if (x + childW > width - paddingRight) {
                x = paddingLeft
                y += rowHeight
                rowHeight = 0
            }

            x += childW
            rowHeight = max(rowHeight, childH)
        }

        setMeasuredDimension(width, y + rowHeight + paddingBottom)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var x = paddingLeft
        var y = paddingTop
        var rowHeight = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue

            val lp = child.layoutParams as MarginLayoutParams
            val w = child.measuredWidth
            val h = child.measuredHeight

            if (x + w + lp.leftMargin + lp.rightMargin > r - l - paddingRight) {
                x = paddingLeft
                y += rowHeight
                rowHeight = 0
            }

            val left = x + lp.leftMargin
            val top = y + lp.topMargin

            child.layout(left, top, left + w, top + h)

            x += w + lp.leftMargin + lp.rightMargin
            rowHeight = max(rowHeight, h + lp.topMargin + lp.bottomMargin)
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }
}
