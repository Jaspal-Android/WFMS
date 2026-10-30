package com.atvantiq.wfms.ui.screens.projectPicker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.ui.screens.projectPicker.ProjectPickerUiState.Phase
import com.atvantiq.wfms.utils.isUnauthorized

/**
 * The project picker's state, kept by the screen's ViewModel so it outlives the sheet (rotation,
 * a theme change). The ViewModel loads the list and reports the answer here; the sheet renders
 * [state] and hands the user's actions to its [ProjectPickerHost]. Main thread only.
 *
 * Attendance is only ever marked against a project the user chose: nothing here sends a request
 * without [selectedProjectId].
 */
class ProjectPicker {

    private val _state = MutableLiveData<ProjectPickerUiState?>()

    /** Null while the picker is closed. */
    val state: LiveData<ProjectPickerUiState?> get() = _state

    val isOpen: Boolean get() = _state.value != null

    val isSubmitting: Boolean get() = _state.value?.isSubmitting == true

    val selectedProjectId: Long? get() = _state.value?.selectedId

    /** Opens the picker for [purpose] (or reopens it for Try again) while its list loads. */
    fun startLoading(purpose: ProjectPickerPurpose) {
        _state.value = ProjectPickerUiState(purpose)
    }

    /**
     * A list answer. `success: false` is a failure even with HTTP 200, shown with the server's
     * message; an empty list is an answer. A lost session closes the picker: the screen asks the
     * user to log in again.
     */
    fun onResponse(success: Boolean?, code: Int?, message: String?, projects: List<ProjectOption>?) {
        when {
            code == ValConstants.UNAUTHORIZED_CODE -> close()
            success == true -> onLoaded(projects.orEmpty())
            else -> onFailed(message)
        }
    }

    /** The list could not be loaded (offline, HTTP error). */
    fun onError(error: Throwable) {
        if (error.isUnauthorized()) close() else onFailed(null)
    }

    fun search(query: String) = update { it.copy(query = query) }

    fun select(projectId: Long) = update { state ->
        if (state.isSubmitting || state.projects.none { it.id == projectId }) state else state.copy(selectedId = projectId)
    }

    fun setSubmitting(submitting: Boolean) = update { it.copy(isSubmitting = submitting) }

    fun close() {
        _state.value = null
    }

    private fun onLoaded(projects: List<ProjectOption>) = update { state ->
        state.copy(
            phase = if (projects.isEmpty()) Phase.EMPTY else Phase.CHOOSE,
            projects = projects,
            // A single project is the only choice; with more, nothing is chosen for the user, so a
            // day is never booked to the wrong project.
            selectedId = projects.singleOrNull()?.id,
            failureMessage = null
        )
    }

    private fun onFailed(message: String?) = update {
        it.copy(phase = Phase.FAILED, projects = emptyList(), selectedId = null, failureMessage = message?.takeIf(String::isNotBlank))
    }

    // An answer that lands after the picker was closed is dropped, so it never reopens.
    private inline fun update(change: (ProjectPickerUiState) -> ProjectPickerUiState) {
        val current = _state.value ?: return
        val next = change(current)
        if (next != current) _state.value = next
    }
}
