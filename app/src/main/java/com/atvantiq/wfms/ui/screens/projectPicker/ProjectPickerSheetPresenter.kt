package com.atvantiq.wfms.ui.screens.projectPicker

import androidx.fragment.app.FragmentManager
import com.atvantiq.wfms.ui.screens.projectPicker.ProjectPickerUiState.Phase

/**
 * Keeps a host's [ProjectPickerBottomSheet] in step with its picker state: while the first list
 * answer loads the host shows its own progress (the button's loading state), then the sheet
 * opens and shows any reload itself. The sheet closes itself when the state does.
 */
class ProjectPickerSheetPresenter(
    private val fragmentManager: () -> FragmentManager,
    private val showProgress: () -> Unit,
    private val dismissProgress: () -> Unit
) {
    private var isShowingProgress = false

    fun render(state: ProjectPickerUiState?) {
        val manager = fragmentManager()
        val sheetShown = manager.findFragmentByTag(ProjectPickerBottomSheet.TAG) != null
        val loadingFirstAnswer = state != null && !sheetShown && state.phase == Phase.LOADING
        if (loadingFirstAnswer != isShowingProgress) {
            isShowingProgress = loadingFirstAnswer
            if (loadingFirstAnswer) showProgress() else dismissProgress()
        }
        val shouldOpen = state != null && !sheetShown && !loadingFirstAnswer
        // showNow, so a state change right after can't open a second sheet.
        if (shouldOpen && !manager.isStateSaved) {
            ProjectPickerBottomSheet().showNow(manager, ProjectPickerBottomSheet.TAG)
        }
    }
}
