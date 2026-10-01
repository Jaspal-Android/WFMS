package com.atvantiq.wfms.utils

import androidx.fragment.app.DialogFragment

/**
 * Sheets and dialogs receive their callbacks from the host after construction. After a
 * configuration change or process death the system restores the fragment without them, so its
 * buttons would silently do nothing. Such a fragment closes itself instead; the host can open it
 * again.
 */
fun DialogFragment.dismissRestoredWithoutCallbacks() {
    dismissAllowingStateLoss()
}

/**
 * Call at the start of `onViewCreated` with the host-set callbacks. Returns true (and dismisses)
 * when any of them is missing, see [dismissRestoredWithoutCallbacks].
 */
fun DialogFragment.dismissIfCallbacksMissing(vararg callbacks: Any?): Boolean {
    if (callbacks.any { it == null }) {
        dismissRestoredWithoutCallbacks()
        return true
    }
    return false
}
