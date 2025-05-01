package com.atvantiq.wfms.base

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.atvantiq.wfms.R
import com.atvantiq.wfms.ui.dialogs.ProgressCircularDialog
import com.atvantiq.wfms.ui.dialogs.ProgressDialog
import com.google.android.material.snackbar.Snackbar


abstract class BaseActivitySimple : AppCompatActivity() {
	
	private  var progressCircularDialog: ProgressCircularDialog ? = null
	private var progressDialog: ProgressDialog? = null

	override fun onCreate(@Nullable savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		getBundle()
	}
	
	open fun getBundle() {
	
	}
	
	fun hideSoftKeyboard(activity: Activity) {
		try {
			val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
			imm.hideSoftInputFromWindow(activity.currentFocus!!.windowToken, 0)
		} catch (exp: Exception) {
		}
		
	}
	
	fun showSnackbar(view: View, message: Int) {
		Snackbar.make(view, getString(message), Snackbar.LENGTH_SHORT).show()
	}
	
	fun showSnackbar(view: View, message: String) {
		Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
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
		builder.setCancelable(false)
		builder.setPositiveButton(getString(R.string.ok), okLister)
		val alertDialog = builder.create()
		alertDialog.show()
	}

	fun alertDialogShow(
		context: Context,
		title: String,
		message: String,
		okLister: DialogInterface.OnClickListener,
	) {
		val builder = AlertDialog.Builder(context)
		builder.setMessage(message)
		builder.setTitle(title)
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
	
	fun alertDialogShow(
		context: Context,
		title: String,
		message: String,
		okButtonTitle: String,
		okLister: DialogInterface.OnClickListener
	) {
		val builder = AlertDialog.Builder(context)
		builder.setTitle(title)
		builder.setMessage(message)
		builder.setPositiveButton(okButtonTitle, okLister)
		builder.setNegativeButton(getString(R.string.cancel), DialogInterface.OnClickListener { dialogInterface, i ->
			dialogInterface.dismiss()
		})
		val alertDialog = builder.create()
		alertDialog.show()
	}


	
	fun showToast(context: Context, message: String) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
	}
	
	fun showCircularProgress() {
		progressCircularDialog = ProgressCircularDialog()
		progressCircularDialog?.show(supportFragmentManager, progressCircularDialog?.tag)
	}
	
	fun dismissCircularProgress() {
		if (progressCircularDialog != null) {
			progressCircularDialog?.dismiss()
			progressCircularDialog = null
		}
	}

	fun showProgress() {
		progressDialog = ProgressDialog()
		progressDialog?.show(supportFragmentManager, "")
	}

	fun dismissProgress() {
		if (progressDialog != null) {
			progressDialog?.dismiss()
			progressDialog=null
		}
	}
	
	fun isLifeCycleResumed(): Boolean = lifecycle.currentState == Lifecycle.State.RESUMED

	fun shakeEditText(context: Context,view: View){
		var animation: Animation = AnimationUtils.loadAnimation(context,R.anim.shake)
		view.startAnimation(animation)
	}
}