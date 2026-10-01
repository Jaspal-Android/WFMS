package com.atvantiq.wfms.base

import com.atvantiq.wfms.utils.isUnauthorized
import com.atvantiq.wfms.constants.ValConstants
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.atvantiq.wfms.R
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.ui.dialogs.ProgressCircularDialog
import com.atvantiq.wfms.ui.dialogs.ProgressDialog
import com.atvantiq.wfms.ui.screens.login.LoginActivity
import com.atvantiq.wfms.utils.SessionCleanup
import com.atvantiq.wfms.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject


open class BaseFragmentSimple : Fragment() {

	@Inject
	lateinit var prefMain: SecurePrefMain

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
	}



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
		title: String,
		message: String,
		okLister: DialogInterface.OnClickListener
	) {
		val builder = AlertDialog.Builder(context)
		builder.setTitle(title)
		builder.setMessage(message)
		builder.setPositiveButton(getString(R.string.ok), okLister)
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
		okLister: DialogInterface.OnClickListener
	) {
		val builder = AlertDialog.Builder(context)
		builder.setMessage(message)
		builder.setTitle(title)
		builder.setPositiveButton(okButtonTitle, okLister)
		val alertDialog = builder.create()
		alertDialog.show()
	}

	fun alertDialogShow(
		context: Context,
		title: String,
		message: String,
		okButtonTitle: String,
		okLister: DialogInterface.OnClickListener,
		isCancelable:Boolean
	) {
		val builder = AlertDialog.Builder(context)
		builder.setMessage(message)
		builder.setTitle(title)
		builder.setCancelable(isCancelable)
		builder.setPositiveButton(okButtonTitle, okLister)
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
		context: Context, title: String, message: String,
		okButtonTitle: String,
		okLister: DialogInterface.OnClickListener,
		neutralButtonTitle: String,
		neutralLister: DialogInterface.OnClickListener
	) {
		MaterialAlertDialogBuilder(context)
			.setTitle(title)
			.setMessage(message)
			.setPositiveButton(okButtonTitle, okLister)
			.setNeutralButton(neutralButtonTitle, neutralLister)
			.setNegativeButton(getString(R.string.cancel), null)
			.show()
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

	lateinit var progressCirluarDialog: ProgressCircularDialog
	private var progressDialog: ProgressDialog? = null

	fun showCircularProgress() {
		// Prevent showing multiple dialogs
		if (::progressCirluarDialog.isInitialized && progressCirluarDialog.isVisible) return
		progressCirluarDialog = ProgressCircularDialog()
		progressCirluarDialog.show(parentFragmentManager, progressCirluarDialog.tag)
	}

	fun dismissCircularProgress() {
		if (::progressCirluarDialog.isInitialized && progressCirluarDialog.isVisible) {
			progressCirluarDialog.dismissAllowingStateLoss()
		}
	}

	fun showProgress() {
		// Prevent showing multiple dialogs
		if (progressDialog?.isVisible == true) return
		progressDialog = ProgressDialog()
		progressDialog?.show(parentFragmentManager, "")
	}

	fun dismissProgress() {
		//if (progressDialog?.isVisible == true) {
			progressDialog?.dismissAllowingStateLoss()
			progressDialog = null
		//}
	}

	fun isLifeCycleResumed() =
		viewLifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED

	fun isLifeCycleStarted() =
		viewLifecycleOwner.lifecycle.currentState == Lifecycle.State.CREATED||viewLifecycleOwner.lifecycle.currentState == Lifecycle.State.STARTED||
				viewLifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED

	fun performLogout(){
		FirebaseMessaging.getInstance().deleteToken()
		SessionCleanup.clearForLogout(requireContext(), prefMain)
		Utils.jumpActivityClearTask(requireContext(), LoginActivity::class.java)
		requireActivity().finish()
	}

	private var sessionExpiredDialog: AlertDialog? = null

	/**
	 * Several requests can fail with 401 at once. Show one dialog, and make it the only way
	 * forward: dismissing it would leave the user on a screen whose every request now fails.
	 */
	fun tokenExpiresAlert() {
		if (!isAdded || sessionExpiredDialog?.isShowing == true) return
		sessionExpiredDialog = MaterialAlertDialogBuilder(requireContext())
			.setTitle(R.string.alert)
			.setMessage(R.string.session_expired_message)
			.setCancelable(false)
			.setPositiveButton(R.string.login) { dialog, _ ->
				dialog.dismiss()
				performLogout()
			}
			.show()
	}

	/**
	 * Reports a failed request: the session-expired dialog for a 401, otherwise the user-facing
	 * message that [com.atvantiq.wfms.base.BaseViewModel] put on the error.
	 */
	fun handleApiFailure(throwable: Throwable?) {
		if (throwable.isUnauthorized()) {
			tokenExpiresAlert()
		} else {
			showToast(requireContext(), throwable?.message ?: getString(R.string.something_went_wrong))
		}
	}

	/** Reports a response the server answered with a non-success `code`. */
	fun handleRejectedResponse(code: Int?, message: String?) {
		if (code == ValConstants.UNAUTHORIZED_CODE) {
			tokenExpiresAlert()
		} else {
			alertDialogShow(requireContext(), getString(R.string.alert), message ?: getString(R.string.something_went_wrong))
		}
	}

	override fun onDestroyView() {
		sessionExpiredDialog?.dismiss()
		sessionExpiredDialog = null
		super.onDestroyView()
	}
}
