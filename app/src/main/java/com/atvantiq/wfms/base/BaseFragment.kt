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
import androidx.lifecycle.AndroidViewModel
import com.atvantiq.wfms.app.MApplication


abstract class BaseFragment<T : ViewDataBinding, V : AndroidViewModel> : BaseFragmentSimple() {
	
	lateinit var binding: T
	lateinit var viewModel: V
	var mContext: Context? = null
	var mActivity: Activity? = null
	
	abstract val fragmentBinding: FragmentBinding
	abstract fun onCreateViewFragment(savedInstanceState: Bundle?)
	protected abstract fun subscribeToEvents(vm: V)
	
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val fragmentBinding = fragmentBinding
		binding = DataBindingUtil.inflate<T>(inflater, fragmentBinding.layoutResId, container, false)
		viewModel = MApplication.provider.create(fragmentBinding.clazz)
		onCreateViewFragment(savedInstanceState)
		subscribeToEvents(viewModel)
		return binding.root
	}
	
	inner class FragmentBinding(
		@param:LayoutRes @field:LayoutRes
		val layoutResId: Int, val clazz: Class<V>
	)
}