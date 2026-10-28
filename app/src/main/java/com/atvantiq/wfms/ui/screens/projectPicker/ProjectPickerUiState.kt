package com.atvantiq.wfms.ui.screens.projectPicker

/** Why the picker is open, which sets its texts and its confirm button. */
sealed interface ProjectPickerPurpose {

    /** A PM starting the day, on one of the projects they manage today. */
    object StartDay : ProjectPickerPurpose

    /** Idle → Present, on one of the active projects; [employeeName] says whose day it is. */
    data class MarkPresent(val employeeName: String?) : ProjectPickerPurpose
}

data class ProjectPickerUiState(
    val purpose: ProjectPickerPurpose,
    val phase: Phase = Phase.LOADING,
    val projects: List<ProjectOption> = emptyList(),
    val query: String = "",
    val selectedId: Long? = null,
    /** The server's reason when it refused the list; null shows the generic text. */
    val failureMessage: String? = null,
    /** The confirmed request (check-in or approval) is being sent. */
    val isSubmitting: Boolean = false
) {
    enum class Phase { LOADING, CHOOSE, EMPTY, FAILED }

    /** The projects whose name matches the search. */
    val visibleProjects: List<ProjectOption>
        get() {
            val search = query.trim()
            return if (search.isEmpty()) projects else projects.filter { it.name.contains(search, ignoreCase = true) }
        }

    val canConfirm: Boolean
        get() = phase == Phase.CHOOSE && selectedId != null && !isSubmitting
}
