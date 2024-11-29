package com.atvantiq.wfms.base

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding


abstract class BaseBindingFragment<T : ViewDataBinding> : BaseFragmentSimple() {
	lateinit var binding: T
	var mContext: Context? = null
	var mActivity: Activity? = null
	
	abstract val fragmentBinding: FragmentBinding
	abstract fun onCreateViewFragment(savedInstanceState: Bundle?)
	
	
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val fragmentBinding = fragmentBinding
		binding = DataBindingUtil.inflate(inflater, fragmentBinding.layoutResId, container, false)
		onCreateViewFragment(savedInstanceState)
		return binding?.root
	}
	
	inner class FragmentBinding(
		@param:LayoutRes @field:LayoutRes
		val layoutResId: Int
	)
	
	override fun onDestroy() {
		super.onDestroy()
		binding?.unbind()
	}
}