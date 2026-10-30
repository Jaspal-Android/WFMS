package com.atvantiq.wfms.ui.screens.projectPicker

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.BottomSheetProjectPickerBinding
import com.atvantiq.wfms.ui.screens.adapters.ProjectOptionAdapter
import com.atvantiq.wfms.ui.screens.projectPicker.ProjectPickerUiState.Phase
import com.atvantiq.wfms.widgets.BaseBottomSheet
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Asks which project attendance is marked against (a PM's Start Day, Idle → Present). It only
 * renders its host's [ProjectPicker] and hands every action to the [ProjectPickerHost], so it
 * survives being recreated; it can't be closed while the confirmed request is being sent.
 */
class ProjectPickerBottomSheet : BaseBottomSheet() {

    private lateinit var binding: BottomSheetProjectPickerBinding
    private var adapter: ProjectOptionAdapter? = null

    private val host: ProjectPickerHost?
        get() = parentFragment as? ProjectPickerHost ?: activity as? ProjectPickerHost

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetProjectPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val host = host
        // Restored without the picker's state (e.g. after process death): nothing to choose from.
        if (host == null || !host.projectPicker.isOpen) {
            dismissAllowingStateLoss()
            return
        }
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
        adapter = ProjectOptionAdapter(onSelect = host.projectPicker::select).also { binding.projectList.adapter = it }
        binding.searchField.doAfterTextChanged { host.projectPicker.search(it?.toString().orEmpty()) }
        host.projectPicker.state.observe(viewLifecycleOwner) { state ->
            if (state == null) dismissAllowingStateLoss() else render(state)
        }
    }

    /** Back or a tap outside. */
    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        host?.onProjectPickerAction(ProjectPickerAction.Dismiss)
    }

    private fun render(state: ProjectPickerUiState) = with(binding) {
        isCancelable = !state.isSubmitting
        val choosing = state.phase == Phase.CHOOSE
        title.text = getString(state.titleRes)
        message.text = state.message(requireContext())
        dialog?.setTitle(title.text)

        searchLayout.isVisible = choosing
        searchField.isEnabled = !state.isSubmitting
        if (searchField.text?.toString().orEmpty() != state.query) searchField.setText(state.query)
        val visible = state.visibleProjects
        listContainer.isVisible = choosing && visible.isNotEmpty()
        noMatches.isVisible = choosing && visible.isEmpty()
        adapter?.submit(visible, state.selectedId)
        loading.isVisible = state.phase == Phase.LOADING

        // Choose: Cancel + confirm. No project: Close only. Couldn't load: Close + Try again.
        btnSecondary.setText(if (choosing || state.phase == Phase.LOADING) R.string.cancel else R.string.close)
        btnSecondary.isEnabled = !state.isSubmitting
        btnSecondary.setOnClickListener { close() }
        primaryContainer.isVisible = choosing || state.phase == Phase.FAILED
        btnPrimary.text = getString(if (choosing) state.purpose.confirmRes else R.string.try_again)
        btnPrimary.isEnabled = if (choosing) state.canConfirm else true
        btnPrimary.visibility = if (state.isSubmitting) View.INVISIBLE else View.VISIBLE
        primaryProgress.isVisible = state.isSubmitting
        btnPrimary.setOnClickListener { if (choosing) confirm() else host?.onProjectPickerAction(ProjectPickerAction.Retry) }
    }

    private fun confirm() {
        val host = host
        val state = host?.projectPicker?.state?.value?.takeIf { it.canConfirm }
        state?.selectedId?.let { host?.onProjectPickerAction(ProjectPickerAction.Confirm(it)) }
    }

    private fun close() {
        val host = host
        if (host?.projectPicker?.isSubmitting != true) {
            dismissAllowingStateLoss()
            host?.onProjectPickerAction(ProjectPickerAction.Dismiss)
        }
    }

    override fun onDestroyView() {
        adapter = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "ProjectPickerBottomSheet"
    }
}

@get:StringRes
private val ProjectPickerUiState.titleRes: Int
    get() = when (phase) {
        Phase.LOADING, Phase.CHOOSE -> R.string.project_picker_title
        Phase.FAILED -> R.string.project_picker_load_failed_title
        Phase.EMPTY -> when (purpose) {
            ProjectPickerPurpose.StartDay -> R.string.project_picker_no_projects_title
            is ProjectPickerPurpose.MarkPresent -> R.string.project_picker_no_active_projects_title
        }
    }

private fun ProjectPickerUiState.message(context: Context): String = when (phase) {
    Phase.LOADING, Phase.CHOOSE -> when (val purpose = purpose) {
        ProjectPickerPurpose.StartDay -> context.getString(R.string.project_picker_start_day_subtitle)
        is ProjectPickerPurpose.MarkPresent -> purpose.employeeName?.takeIf { it.isNotBlank() }
            ?.let { context.getString(R.string.project_picker_mark_present_subtitle, it) }
            ?: context.getString(R.string.project_picker_mark_present_subtitle_unnamed)
    }
    Phase.FAILED -> failureMessage ?: context.getString(R.string.project_picker_load_failed_message)
    Phase.EMPTY -> context.getString(
        when (purpose) {
            ProjectPickerPurpose.StartDay -> R.string.project_picker_no_projects_message
            is ProjectPickerPurpose.MarkPresent -> R.string.project_picker_no_active_projects_message
        }
    )
}

@get:StringRes
private val ProjectPickerPurpose.confirmRes: Int
    get() = when (this) {
        ProjectPickerPurpose.StartDay -> R.string.start_your_day
        is ProjectPickerPurpose.MarkPresent -> R.string.mark_present
    }
