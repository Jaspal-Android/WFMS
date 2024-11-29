package com.atvantiq.wfms.base

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.annotation.Nullable
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


abstract class BaseActivity<T : ViewDataBinding, V : ViewModel> : BaseActivitySimple() {

    lateinit var binding: T
    lateinit var viewModel: V


    abstract val bindingActivity: ActivityBinding

    abstract fun onCreateActivity(savedInstanceState: Bundle?)

    protected abstract fun subscribeToEvents(vm: V)

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activityBinding = bindingActivity
        binding = DataBindingUtil.setContentView(this, activityBinding.layoutResId)
        viewModel = ViewModelProvider(this)[activityBinding.clzazz]
        onCreateActivity(savedInstanceState)
        subscribeToEvents(viewModel)
    }

    inner class ActivityBinding(
        @param:LayoutRes @field:LayoutRes val layoutResId: Int, val clzazz: Class<V>
    )
}