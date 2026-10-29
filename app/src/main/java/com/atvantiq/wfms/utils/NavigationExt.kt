package com.atvantiq.wfms.utils

import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navOptions

/**
 * Switches to a bottom-navigation tab from code, exactly as tapping it does: the current tab's
 * state is saved, the target tab's saved state is restored, and the bottom bar follows.
 */
fun NavController.navigateToTab(@IdRes tabId: Int) {
    navigate(tabId, null, navOptions {
        launchSingleTop = true
        restoreState = true
        popUpTo(graph.findStartDestination().id) { saveState = true }
    })
}
