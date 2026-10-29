package com.atvantiq.wfms.utils

import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import com.ncorti.slidetoact.SlideToActView

/**
 * Start/End Day is a [SlideToActView], which can only be operated by dragging. TalkBack, Switch
 * Access and many motor impairments cannot drag, so those users could not mark attendance at all.
 * Announce the current action and let an accessibility service trigger the same completion
 * callback with a plain "activate".
 */
fun SlideToActView.setAccessibleAction(label: CharSequence) {
    contentDescription = label
    ViewCompat.replaceAccessibilityAction(this, AccessibilityActionCompat.ACTION_CLICK, label) { _, _ ->
        onSlideCompleteListener?.onSlideComplete(this)
        true
    }
}
