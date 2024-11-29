package com.atvantiq.wfms.ui.screens.feedback

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.databinding.FragmentFeedbackBinding

class FeedbackFragment : BaseFragment<FragmentFeedbackBinding,FeedbackViewModel>() {

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_feedback,FeedbackViewModel::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {

    }

    override fun subscribeToEvents(vm: FeedbackViewModel) {

    }

}