package com.atvantiq.wfms.ui.dialogs

import android.os.Bundle
import androidx.fragment.app.DialogFragment

/**
 * A progress dialog belongs to the request that showed it. When the screen is recreated (theme
 * change, rotation, process restore) Android restores the dialog, but the recreated screen no
 * longer holds it, so nothing would ever dismiss it and it would block the screen for good. A
 * restored one removes itself instead; a screen whose request is still running shows a new one
 * when it redraws its loading state.
 *
 * Call from `onCreate`, after `super.onCreate`.
 */
internal fun DialogFragment.dropIfRestored(savedInstanceState: Bundle?) {
    if (savedInstanceState == null) return
    showsDialog = false
    dismissAllowingStateLoss()
}
