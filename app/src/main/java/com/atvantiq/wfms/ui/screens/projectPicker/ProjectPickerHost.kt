package com.atvantiq.wfms.ui.screens.projectPicker

/**
 * The screen that shows [ProjectPickerBottomSheet]: the sheet finds it as its parent fragment, or
 * else its activity, and hands it every action.
 */
interface ProjectPickerHost {

    val projectPicker: ProjectPicker

    fun onProjectPickerAction(action: ProjectPickerAction)
}

sealed interface ProjectPickerAction {

    /** Try again after the list failed to load. */
    object Retry : ProjectPickerAction

    /** The user confirmed [projectId]; the host sends its request and closes the picker on success. */
    data class Confirm(val projectId: Long) : ProjectPickerAction

    /** Cancel, Close, back or a tap outside: nothing is sent. */
    object Dismiss : ProjectPickerAction
}
