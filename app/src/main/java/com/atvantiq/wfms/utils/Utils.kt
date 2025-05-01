package com.atvantiq.wfms.utils

import android.app.Activity
import android.content.*
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.atvantiq.wfms.R
import com.google.gson.Gson
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


object Utils {
	
	@Synchronized
	fun <T> getIntent(context: Context, clazz: Class<T>, bundle: Bundle): Intent {
		val intent = Intent(context, clazz)
		intent.putExtras(bundle)
		return intent
	}
	
	@Synchronized
	fun <T> getIntent(context: Context, clazz: Class<T>): Intent {
		val intent = Intent(context, clazz)
		return intent
	}
	
	@Synchronized
	fun <T> jumpActivityClearTask(context: Context, clazz: Class<T>) {
		val intent = Intent(context, clazz)
		intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
		context.startActivity(intent)
	}
	
	@Synchronized
	fun <T> jumpActivityClearTask(context: Context, clazz: Class<T>, bundle: Bundle) {
		val intent = Intent(context, clazz)
		intent.putExtras(bundle)
		intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
		context.startActivity(intent)
	}
	
	@Synchronized
	fun <T> jumpActivity(context: Context, clazz: Class<T>) {
		val intent = Intent(context, clazz)
		context.startActivity(intent)
	}
	
	@Synchronized
	fun <T> jumpActivityForResult(context: Activity, clazz: Class<T>, resultCode: Int) {
		val intent = Intent(context, clazz)
		context.startActivityForResult(intent, resultCode)
	}
	
	@Synchronized
	fun <T> jumpActivityWithData(context: Context, clazz: Class<T>, bundle: Bundle) {
		val intent = Intent(context, clazz)
		intent.putExtras(bundle)
		context.startActivity(intent)
	}
	
	@Synchronized
	fun <T> jumpActivityWithAction(context: Context, clazz: Class<T>, action: String) {
		val intent = Intent(context, clazz)
		intent.action = action
		context.startActivity(intent)
	}
	
	@Synchronized
	fun <T> jumpActivityForResult(
		context: Activity,
		resultCode: Int,
		clazz: Class<T>,
		bundle: Bundle
	) {
		val intent = Intent(context, clazz)
		intent.putExtras(bundle)
		context.startActivityForResult(intent, resultCode)
	}
	
	
	fun switchFragment(
		clearStack: Boolean,
		fm: FragmentManager,
		frame: Int,
		fragment: Fragment,
		isStacked: Boolean
	) {
		val tag = fragment.javaClass.simpleName
		
		if (clearStack) {
			fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
		}
		val transaction = fm.beginTransaction()
		transaction.replace(frame, fragment, tag)
		if (isStacked) {
			transaction.addToBackStack(tag)
		}
		transaction.commit()
	}
	
	fun hideSoftKeyBoard(context: Activity) {
		context.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
		
	}
	
	fun hideKeyboardOnClick(context: Context, view: View) {
		val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
		imm.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY)
	}
	
	fun modelToString(`object`: Any): String {
		val gson = Gson()
		return gson.toJson(`object`)
	}
	
	fun <T> stringToModel(json: String, clazz: Class<T>): Any {
		val gson = Gson()
		return gson.fromJson(json, clazz)!!
	}
	
	fun dateToString(date: String): String {
		var outDate: Date? = null
		val formatIn = SimpleDateFormat("yyyy-mm-dd'T'hh:mm:ss")
		val formatOut = SimpleDateFormat("dd/mm/yyyy hh:mm:ss a")
		try {
			outDate = formatIn.parse(date)
		} catch (e: ParseException) {
			e.printStackTrace()
		}
		
		return formatOut.format(outDate)
	}

	fun isInternet(context: Context): Boolean {
		return ConnectivityReceiver.isNetworkAvailable(context)
	}
	
	fun getSoftButtonsBarSizePort(activity: Activity): Int {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			val metrics = DisplayMetrics()
			activity.windowManager.defaultDisplay.getMetrics(metrics)
			val usableHeight = metrics.heightPixels
			activity.windowManager.defaultDisplay.getRealMetrics(metrics)
			val realHeight = metrics.heightPixels
			return if (realHeight > usableHeight)
				realHeight - usableHeight
			else
				0
		}
		return 0
	}
	
	/*fun errorHandlingWithStatus(context: Context, e: Throwable): ErrorResponse {
		var errorResponse = ErrorResponse(context.getString(R.string.exception_msg), "")
		if (e is HttpException) {
			val response = e.response()
			try {
				val jObjError = JSONObject(response.errorBody()?.string())
				errorResponse.message = jObjError.optString("message")
				errorResponse.statusCode = jObjError.optString("status")
			} catch (e1: JSONException) {
				errorResponse.message = e1.localizedMessage
				e1.printStackTrace()
			} catch (e1: IOException) {
				errorResponse.message = e1.localizedMessage
				e1.printStackTrace()
			}
		}
		return errorResponse
	}*/
	
	fun pxToDp(px: Int, context: Context): Int {
		val displayMetrics: DisplayMetrics = context.resources.displayMetrics
		return Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))
	}
	
	fun dpToPx(dp: Float, context: Context): Int {
		val displayMetrics: DisplayMetrics = context.resources.displayMetrics
		return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))
	}
	
	fun hidePassword(et: EditText?) {
		et?.transformationMethod = HideReturnsTransformationMethod.getInstance()
		et?.setSelection(et.text.length)
	}
	
	fun showPassword(et: EditText?) {
		et?.transformationMethod = PasswordTransformationMethod()
		et?.setSelection(et.text.length)
	}

	/*
	* Rate Us Dialog
	* */
	fun rateUsDialog(context: Context) {
		val uri: Uri = Uri.parse("market://details?id=" + context.packageName)
		val goToMarket = Intent(Intent.ACTION_VIEW, uri)
		goToMarket.addFlags(
			Intent.FLAG_ACTIVITY_NO_HISTORY or
					Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
					Intent.FLAG_ACTIVITY_MULTIPLE_TASK
		)
		try {
			context.startActivity(goToMarket)
		} catch (e: ActivityNotFoundException) {
			context.startActivity(
				Intent(
					Intent.ACTION_VIEW,
					Uri.parse("http://play.google.com/store/apps/details?id=" + context.packageName)
				)
			)
		}
	}
	
	/*
	* Call to telephone number
	* */
	fun callNumber(context: Context, tellPhone: String) {
		var callIntent = Intent(Intent.ACTION_DIAL)
		callIntent.data = Uri.parse("tel:$tellPhone")
		context.startActivity(callIntent)
	}

	fun openAppSettings(context: Context) {
		val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
		val uri = Uri.fromParts("package", "com.atvantiq.parqngo", null)
		intent.data = uri
		context.startActivity(intent)
	}
	
	/*
	 *Share intent method
	 * */
	fun shareContent(context: Context, shareContent: String) {
		var shareIntent = Intent(Intent.ACTION_SEND)
		shareIntent.type = "text/plain"
		shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent)
		shareIntent.addFlags(
			Intent.FLAG_ACTIVITY_NO_HISTORY or
					Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
					Intent.FLAG_ACTIVITY_MULTIPLE_TASK
		)
		
		context.startActivity(
			Intent.createChooser(
				shareIntent,
				context.getString(R.string.app_name)
			)
		)
	}

	fun shareApp(context: Context){
		val shareIntent = Intent(Intent.ACTION_SEND).apply {
			type = "text/plain"
			putExtra(Intent.EXTRA_SUBJECT, "Check out this awesome app!")
			val appPackageName = "com.atvantiq.parqngo"  // Get your app's package name
			val playStoreLink = "https://play.google.com/store/apps/details?id=$appPackageName"
			putExtra(Intent.EXTRA_TEXT, "Download this app from the Play Store: $playStoreLink")
		}
		context.startActivity(Intent.createChooser(shareIntent, "Share app via"))
	}

	fun roundOffDecimal(number: Double): Double? {
		return try {
			val df = DecimalFormat("#.##", DecimalFormatSymbols(Locale.ENGLISH))
			df.roundingMode = RoundingMode.FLOOR
			df.format(number).toDouble()
		} catch (e: NumberFormatException) {
			0.0
		}
	}
	
	fun formatToString(format: String, value: Double?): String {
		return String.format(Locale.ENGLISH, format, value)
	}

	fun getBitmapFromView(view: View): Bitmap {
		view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
		view.layout(0, 0, view.measuredWidth, view.measuredHeight)
		val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
		val canvas = Canvas(bitmap)
		view.draw(canvas)
		return bitmap
	}

	fun getGreeting(context: Context): String {
		val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
		val resources = context.resources

		return when (hour) {
			in 5..11 -> resources.getString(R.string.greeting_morning)
			in 12..16 -> resources.getString(R.string.greeting_afternoon)
			in 17..20 -> resources.getString(R.string.greeting_evening)
			else -> resources.getString(R.string.greeting_night)
		}
	}
}

