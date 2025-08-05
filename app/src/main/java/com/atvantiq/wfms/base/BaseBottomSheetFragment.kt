package com.atvantiq.wfms.base

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.atvantiq.wfms.R
import com.atvantiq.wfms.app.MApplication
import com.atvantiq.wfms.ui.dialogs.ProgressCircularDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar


abstract class BaseBottomSheetFragment<T : ViewDataBinding, V : AndroidViewModel> :
	BottomSheetDialogFragment() {
	
	lateinit var binding: T
	lateinit var viewModel: V
	var mContext: Context? = null
	var mActivity: Activity? = null
	lateinit var progressCirluarDialog: ProgressCircularDialog
	abstract val fragmentBinding: FragmentBinding
	abstract fun onCreateViewFragment(savedInstanceState: Bundle?)
	protected abstract fun subscribeToEvents(vm: V)
	
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
	}
	
	
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val fragmentBinding = fragmentBinding
		binding = DataBindingUtil.inflate(inflater, fragmentBinding.layoutResId, container, false)
		viewModel = ViewModelProvider(this)[fragmentBinding.clazz]
		onCreateViewFragment(savedInstanceState)
		subscribeToEvents(viewModel)
		return binding.root
	}
	
	private fun initProgress() {
		progressCirluarDialog = ProgressCircularDialog()
	}
	
	inner class FragmentBinding(
		@param:LayoutRes @field:LayoutRes
		val layoutResId: Int, val clazz: Class<V>
	)
	
	fun showSnackbar(view: View, messsage: Int) {
		Snackbar.make(view, getString(messsage), Snackbar.LENGTH_SHORT).show()
	}
	
	fun showSnackbar(view: View, messsage: String) {
		Snackbar.make(view, messsage, Snackbar.LENGTH_SHORT).show()
	}

	fun alertDialogShow(context: Context, message: String) {
		val builder = AlertDialog.Builder(context)
		builder.setMessage(message)
		builder.setPositiveButton(getString(R.string.ok)) { dialogInterface, i -> dialogInterface.dismiss() }
		val alertDialog = builder.create()
		alertDialog.show()
	}
	
	fun alertDialogShow(context: Context, title: String, message: String) {
		val builder = AlertDialog.Builder(context)
		builder.setMessage(message)
		builder.setTitle(title)
		builder.setPositiveButton(getString(R.string.ok)) { dialogInterface, i -> dialogInterface.dismiss() }
		val alertDialog = builder.create()
		alertDialog.show()
	}
	
	fun alertDialogShow(
		context: Context,
		message: String,
		okLister: DialogInterface.OnClickListener
	) {
		val builder = AlertDialog.Builder(context)
		builder.setMessage(message)
		builder.setPositiveButton(getString(R.string.ok), okLister)
		val alertDialog = builder.create()
		alertDialog.show()
	}
	
	fun alertDialogShow(
		context: Context,
		title: String,
		message: String,
		okButtonTitle: String,
		okLister: DialogInterface.OnClickListener,
		canelLister: DialogInterface.OnClickListener
	) {
		val builder = AlertDialog.Builder(context)
		builder.setMessage(message)
		builder.setTitle(title)
		builder.setPositiveButton(okButtonTitle, okLister)
		builder.setNegativeButton(getString(R.string.cancel), canelLister)
		val alertDialog = builder.create()
		alertDialog.show()
	}
	
	fun showToast(context: Context, message: String) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
	}
	
	fun setItemsVisibility(menu: Menu, exception: MenuItem, visible: Boolean?) {
		for (i in 0 until menu.size()) {
			val item = menu.getItem(i)
			if (item !== exception) {
				item.isVisible = visible!!
			}
		}
	}
	
	override fun onDestroy() {
		super.onDestroy()
		binding.unbind()
	}
	
	
	fun hideKeyboard(activity: Activity) {
		val inputManager = activity
			.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
		
		// check if no view has focus:
		val currentFocusedView = activity.currentFocus
		if (currentFocusedView != null) {
			inputManager.hideSoftInputFromWindow(
				currentFocusedView.windowToken,
				InputMethodManager.HIDE_NOT_ALWAYS
			)
		}
	}
	
	fun showCircularProgress() {
		progressCirluarDialog = ProgressCircularDialog()
		progressCirluarDialog.show(requireFragmentManager(), "")
	}
	
	fun dismissCircularProgress() {
		if (progressCirluarDialog != null) {
			progressCirluarDialog.dismiss()
		}
	}
	
	override fun onDestroyView() {
		super.onDestroyView()
		if (binding != null) {
			binding.unbind()
		}
	}
	
	fun isLifeCycleResumed() =
		viewLifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED
}