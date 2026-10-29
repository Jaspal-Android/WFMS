package com.atvantiq.wfms.utils

import android.app.Activity
import android.content.*
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.provider.Settings
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.DisplayMetrics
import android.view.View
import android.widget.EditText
import com.atvantiq.wfms.R
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors


object Utils {

    // Attendance actions must use a recent, accurate fix. Shared by the employee and
    // admin dashboards so the freshness/accuracy thresholds never drift apart.
    private const val ATTENDANCE_LOCATION_MAX_AGE_MILLIS = 2 * 60 * 1000L
    private const val ATTENDANCE_LOCATION_MAX_ACCURACY_METERS = 100f

    fun isUsableAttendanceLocation(location: android.location.Location?): Boolean {
        if (location == null) return false
        val ageMillis = System.currentTimeMillis() - location.time
        return ageMillis in 0..ATTENDANCE_LOCATION_MAX_AGE_MILLIS &&
                location.accuracy <= ATTENDANCE_LOCATION_MAX_ACCURACY_METERS
    }

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
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        context.startActivity(intent)
    }

    @Synchronized
    fun <T> jumpActivityClearTask(context: Context, clazz: Class<T>, bundle: Bundle) {
        val intent = Intent(context, clazz)
        intent.putExtras(bundle)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
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


    fun modelToString(`object`: Any): String {
        val gson = Gson()
        return gson.toJson(`object`)
    }

    fun <T> stringToModel(json: String, clazz: Class<T>): Any {
        val gson = Gson()
        return gson.fromJson(json, clazz)!!
    }

    fun isInternet(context: Context): Boolean {
        return ConnectivityReceiver.isNetworkAvailable(context)
    }

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

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", context.packageName, null)
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

    private val geocodeExecutor: Executor =
        Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "geocoder").apply { isDaemon = true } }

    private val mainThreadExecutor: Executor by lazy {
        val handler = Handler(Looper.getMainLooper())
        Executor { handler.post(it) }
    }

    /**
     * Looks up the address for a location. Never blocks the caller, and always calls [onResult] on
     * the main thread with either the address or the "address not found" text.
     */
    @Suppress("DEPRECATION")
    fun getAddressFromLatLong(
        context: Context,
        latitude: Double,
        longitude: Double,
        onResult: (String) -> Unit
    ) {
        val appContext = context.applicationContext
        val notFound = appContext.getString(R.string.address_not_found)
        val geocoder = Geocoder(appContext, Locale.getDefault())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    val address = addresses.firstOrNull()?.let { formatAddress(it) } ?: notFound
                    mainThreadExecutor.execute { onResult(address) }
                }

                override fun onError(errorMessage: String?) {
                    mainThreadExecutor.execute { onResult(notFound) }
                }
            })
        } else {
            AddressResolver(
                geocode = { lat, lon -> geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()?.let { formatAddress(it) } },
                background = geocodeExecutor,
                mainThread = mainThreadExecutor,
                notFound = notFound
            ).resolve(latitude, longitude, onResult)
        }
    }


    private fun formatAddress(address: Address?): String {
        return buildString {
            address?.let {
                if (!it.featureName.isNullOrEmpty()) append(it.featureName + ", ")
                if (!it.thoroughfare.isNullOrEmpty()) append(it.thoroughfare + ", ")
                if (!it.subLocality.isNullOrEmpty()) append(it.subLocality + ", ")
                if (!it.locality.isNullOrEmpty()) append(it.locality + ", ")
                if (!it.adminArea.isNullOrEmpty()) append(it.adminArea + ", ")
                if (!it.postalCode.isNullOrEmpty()) append(it.postalCode + ", ")
                if (!it.countryName.isNullOrEmpty()) append(it.countryName)
            }
        }.trim().trimEnd(',')
    }

    /**
     * The prominent disclosure shown immediately before every location permission request (Google
     * Play's prominent-disclosure policy). One button, Continue, which always goes straight to
     * the system request: Apple rejected a Cancel button here (Guideline 5.1.1(iv)), and the system
     * prompt is where the user allows or declines. Not cancelable by tapping outside or Back.
     */
    fun showLocationDisclosureDialog(
        context: Context,
        title: String,
        message: String,
        onContinue: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(context.getString(R.string.continue_label)) { dialog, _ ->
                dialog.dismiss()
                onContinue()
            }
            .show()
    }

    fun applyGradient(view: View) {
        val priD = MaterialColors.getColor(view, R.attr.wfmsColorPrimaryDark)
        val pri  = MaterialColors.getColor(view, R.attr.wfmsColorPrimary)
        val gE   = MaterialColors.getColor(view, R.attr.wfmsColorGradientEnd)

        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(priD, pri, gE)
        )
        view.background = gradient
    }
    fun applyCircularGradient(view: View) {
        val priD = MaterialColors.getColor(view, R.attr.wfmsColorPrimaryDark)
        val pri  = MaterialColors.getColor(view, R.attr.wfmsColorPrimary)
        val gE   = MaterialColors.getColor(view, R.attr.wfmsColorGradientEnd)

        val gd = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(priD, pri, gE)
        ).apply {
            shape = GradientDrawable.OVAL
        }
        view.background = gd
    }

    /** Two initials for an avatar, as on iOS: "kamal sharma" → "KS", a single name "Jaspal" → "JA". */
    fun nameInitials(name: String?): String {
        val words = name.orEmpty().trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when (words.size) {
            0 -> ""
            1 -> words.first().take(INITIALS_LENGTH).uppercase()
            else -> "${words.first().first()}${words.last().first()}".uppercase()
        }
    }

    private const val INITIALS_LENGTH = 2
}
