package com.atvantiq.wfms.ui.screens.more

import android.os.Bundle
import android.view.View
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.databinding.FragmentMoreBinding
import com.atvantiq.wfms.ui.dialogs.ThemePickerBottomSheet
import com.atvantiq.wfms.utils.Utils
import com.atvantiq.wfms.utils.isSessionLost
import dagger.hilt.android.AndroidEntryPoint

/** More tab: profile card, View Profile, Appearance and Logout. */
@AndroidEntryPoint
class MoreFragment : BaseFragment<FragmentMoreBinding, ProfileVM>() {

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_more, ProfileVM::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.refresh()
    }

    override fun subscribeToEvents(vm: ProfileVM) {
        binding.vm = vm
        vm.profile.observe(viewLifecycleOwner) { profile -> binding.profile = profile }

        vm.profileResponse.observe(viewLifecycleOwner) { response ->
            if (!response.consumeOnce()) return@observe
            // The cached profile stays on screen if the refresh fails; only a lost session matters.
            if (response.isSessionLost { it.code }) tokenExpiresAlert()
        }

        vm.clickEvents.observe(viewLifecycleOwner) { event ->
            if (!isLifeCycleResumed()) return@observe
            when (event) {
                MoreClickEvents.VIEW_PROFILE -> Utils.jumpActivity(requireContext(), ProfileActivity::class.java)
                MoreClickEvents.APPEARANCE -> ThemePickerBottomSheet().show(parentFragmentManager, THEME_PICKER_TAG)
                MoreClickEvents.LOGOUT -> confirmLogout()
            }
        }
    }

    private fun confirmLogout() {
        alertDialogShow(
            requireContext(),
            getString(R.string.logout),
            getString(R.string.logout_confirmation),
            getString(R.string.yes),
            { dialog, _ ->
                dialog.dismiss()
                performLogout()
            },
            { dialog, _ -> dialog.dismiss() }
        )
    }

    private companion object {
        const val THEME_PICKER_TAG = "ThemePicker"
    }
}
