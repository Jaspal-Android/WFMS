package com.atvantiq.wfms.base

import GenericBottomSheetDialog
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
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.ui.dialogs.ProgressCircularDialog
import com.atvantiq.wfms.ui.dialogs.ProgressDialog
import com.atvantiq.wfms.ui.screens.login.LoginActivity
import com.atvantiq.wfms.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
abstract class BaseActivitySimple : AppCompatActivity() {

    @Inject
    lateinit var prefMain: SecurePrefMain

    private var progressCircularDialog: ProgressCircularDialog? = null
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
        builder.setNegativeButton(
            getString(R.string.cancel),
            DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
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
            progressDialog = null
        }
    }

    fun isLifeCycleResumed(): Boolean = lifecycle.currentState == Lifecycle.State.RESUMED

    fun shakeEditText(context: Context, view: View) {
        var animation: Animation = AnimationUtils.loadAnimation(context, R.anim.shake)
        view.startAnimation(animation)
    }

    private fun performGlobalLogout() {
        FirebaseMessaging.getInstance().deleteToken()
        prefMain.deleteAll()
        Utils.jumpActivity(this, LoginActivity::class.java)
        finish()
    }

    fun tokenExpiresAlert() {
        alertDialogShow(
            this,
            getString(R.string.alert),
            getString(R.string.unauthorized_access),
            getString(R.string.login),
            DialogInterface.OnClickListener() { dialog, which ->
                dialog.dismiss()
                performGlobalLogout()
            })
    }

    fun <T> showSelectionDialog(
        items: List<T>,
        title: String,
        layoutResId: Int,
        bind: (view: android.view.View, item: T) -> Unit,
        onItemSelected: (T) -> Unit,
        filterCondition: (T, String) -> Boolean,
        emptyMessage: String,
        retryAction: () -> Unit,
        tag: String
    ) {
        if (items.isNotEmpty()) {
            val dialog = GenericBottomSheetDialog(
                context = this,
                items = items,
                layoutResId = layoutResId,
                bind = bind,
                onItemSelected = {
                    onItemSelected(it)
                },
                filterCondition = filterCondition,
                title = title
            )
            dialog.show(supportFragmentManager, tag)
        } else {
            alertDialogShow(
                this,
                getString(R.string.alert),
                emptyMessage,
                getString(R.string.retry),
                okLister = DialogInterface.OnClickListener { _, _ -> retryAction() },
            )
        }
    }

}