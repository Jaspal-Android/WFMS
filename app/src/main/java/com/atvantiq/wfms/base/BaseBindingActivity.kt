package com.atvantiq.wfms.base

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.annotation.Nullable
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding


abstract class BaseBindingActivity<T : ViewDataBinding> : BaseActivitySimple() {
	
	lateinit var binding: T
	abstract val bindingActivity: ActivityBinding
	abstract fun onCreateActivity(savedInstanceState: Bundle?)
	
	override fun onCreate(@Nullable savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val activityBinding = bindingActivity
		binding = DataBindingUtil.setContentView(this, activityBinding.layoutResId)
		onCreateActivity(savedInstanceState)
	}
	
	override fun onDestroy() {
		super.onDestroy()
		binding.unbind()
	}
	
	inner class ActivityBinding(
		@param:LayoutRes
		val layoutResId: Int
	)
}