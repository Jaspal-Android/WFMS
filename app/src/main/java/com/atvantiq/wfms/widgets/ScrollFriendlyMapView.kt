package com.atvantiq.wfms.widgets

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.maps.MapView

/**
 * A [MapView] that can sit inside scrolling or paging parents: while a finger is on the map, the
 * parents don't steal the gesture, so panning and zooming work.
 */
class ScrollFriendlyMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : MapView(context, attrs) {

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> parent?.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> parent?.requestDisallowInterceptTouchEvent(false)
        }
        return super.dispatchTouchEvent(event)
    }

    /** Forwards the host's lifecycle to the map, as MapView requires. */
    fun bindLifecycle(owner: LifecycleOwner, savedInstanceState: Bundle?) {
        onCreate(savedInstanceState)
        owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = this@ScrollFriendlyMapView.onStart()
            override fun onResume(owner: LifecycleOwner) = this@ScrollFriendlyMapView.onResume()
            override fun onPause(owner: LifecycleOwner) = this@ScrollFriendlyMapView.onPause()
            override fun onStop(owner: LifecycleOwner) = this@ScrollFriendlyMapView.onStop()
            override fun onDestroy(owner: LifecycleOwner) = this@ScrollFriendlyMapView.onDestroy()
        })
    }
}
