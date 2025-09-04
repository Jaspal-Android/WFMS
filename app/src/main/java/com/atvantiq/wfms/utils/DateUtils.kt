package com.atvantiq.wfms.utils

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.TimePicker
import com.atvantiq.wfms.R
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private const val DATE_FORMAT = "yyyy-MM-dd"
    private const val API_DATE_FORMAT = "yyyy.MM.dd.HH.mm.ss"
    private const val DOB_FORMAT = "dd/MM/yyyy"
    private const val DISPLAY_DATE_FORMAT = "dd/MM/yyyy"
    private const val CURRENT_DATE_FORMAT = "dd-MM-yyyy"
    private const val CURRENT_TIME_FORMAT = "hh:mm a"
    private const val TIME_24_FORMAT = "HH:mm:ss"
    private const val TIME_12_FORMAT = "hh:mm a"
    private const val API_ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    private const val API_DISPLAY_FORMAT = "hh:mm a  dd-MM-yyyy"

    fun onDateClick(context: Context, callBack: DateCallBack) {
        val c = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            context, R.style.AppTheme_DatePickerDialog,
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                val calendar = Calendar.getInstance().apply {
                    set(year, monthOfYear, dayOfMonth)
                }
                val format = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
                val formatapi = SimpleDateFormat(API_DATE_FORMAT, Locale.getDefault())
                val strDate = format.format(calendar.time)
                val formatDate = formatapi.format(calendar.time)
                callBack.onDateSelected(strDate, formatDate)
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    fun onDOBPickerClick(context: Context, callBack: DateCallBack) {
        val calendar = Calendar.getInstance().apply { set(1990, 1, 1) }
        val datePickerDialog = DatePickerDialog(
            context, R.style.AppTheme_DatePickerDialog,
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, monthOfYear, dayOfMonth)
                }
                val format = SimpleDateFormat(DOB_FORMAT, Locale.getDefault())
                val strDate = format.format(selectedCalendar.time)
                callBack.onDateSelected(strDate, strDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.maxDate = Date().time
        datePickerDialog.show()
    }

    fun onDateClickWithLimit(
        context: Context,
        callBack: DateCallBack,
        isAfterCurrentDate: Boolean
    ) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            context, R.style.AppTheme_DatePickerDialog,
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, monthOfYear, dayOfMonth)
                }
                val format = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
                val formatapi = SimpleDateFormat(API_DATE_FORMAT, Locale.getDefault())
                val strDate = format.format(selectedCalendar.time)
                val formatDate = formatapi.format(selectedCalendar.time)
                callBack.onDateSelected(strDate, formatDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        if (isAfterCurrentDate) {
            datePickerDialog.datePicker.minDate = calendar.timeInMillis
        } else {
            datePickerDialog.datePicker.maxDate = calendar.timeInMillis
        }
        datePickerDialog.show()
    }

    fun showTimerPicker(context: Context, callBack: TimeCallBack) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val timePickerDialog = TimePickerDialog(
            context, R.style.AppTheme_DatePickerDialog,
            { _: TimePicker, selectedHour: Int, selectedMinute: Int ->
                val format = if (selectedHour >= 12) "PM" else "AM"
                val hourIn12Format = when {
                    selectedHour == 0 -> 12
                    selectedHour > 12 -> selectedHour - 12
                    else -> selectedHour
                }
                val time = String.format("%d:%02d %s", hourIn12Format, selectedMinute, format)
                val formatTime = String.format("%d:%02d", hourIn12Format, selectedMinute)
                callBack.onTimeSelected(time, formatTime)
            }, hour, minute, false
        )
        timePickerDialog.show()
    }

    @SuppressLint("SimpleDateFormat")
    fun getCurrentDate(): String {
        val c = Calendar.getInstance().time
        val df = SimpleDateFormat(CURRENT_DATE_FORMAT, Locale.getDefault())
        return df.format(c)
    }

    @SuppressLint("SimpleDateFormat")
    fun getCurrentTime(): String {
        val c = Calendar.getInstance().time
        val df = SimpleDateFormat(CURRENT_TIME_FORMAT, Locale.getDefault())
        val formatted = df.format(c)
        return formatted.replace(Regex("am|pm", RegexOption.IGNORE_CASE)) { it.value.uppercase() }
    }

    @SuppressLint("SimpleDateFormat")
    fun formatDate(date: String?): String {
        if (date.isNullOrEmpty()) return ""
        return try {
            val df = SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS", Locale.getDefault())
            val c = df.parse(date)
            val df1 = SimpleDateFormat(DISPLAY_DATE_FORMAT, Locale.getDefault())
            df1.format(c)
        } catch (e: Exception) {
            ""
        }
    }

    fun getCurrentMonthAndYear(): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        return Pair(month, year)
    }

    fun convertFrom24(inTime: String): String {
        return try {
            val timeFormat24 = SimpleDateFormat(TIME_24_FORMAT, Locale.getDefault())
            val timeFormat12 = SimpleDateFormat(TIME_12_FORMAT, Locale.getDefault())
            val inTimeParsed = timeFormat24.parse(inTime)
            val formatted = timeFormat12.format(inTimeParsed)
            formatted.replace(Regex("am|pm", RegexOption.IGNORE_CASE)) { it.value.uppercase() }
        } catch (e: Exception) {
            ""
        }
    }

    interface DateCallBack {
        fun onDateSelected(date: String, formatDate: String)
    }

    interface TimeCallBack {
        fun onTimeSelected(time: String, formatTime: String)
    }

    fun formatApiDateToYMD(apiDate: String?): String? {
        return try {
            val trimmed = apiDate?.substringBefore(".")?.plus("Z")
            val isoFormat = SimpleDateFormat(API_ISO_FORMAT, Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val parsedDate = isoFormat.parse(trimmed)
            val outputFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            outputFormat.format(parsedDate)
        } catch (e: Exception) {
            null
        }
    }

    fun formatApiDateToTimeAndDate(apiDate: String?): String? {
        return try {
            val trimmed = apiDate?.substringBefore(".")?.plus("Z")
            val isoFormat = SimpleDateFormat(API_ISO_FORMAT, Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val parsedDate = isoFormat.parse(trimmed)
            val outputFormat = SimpleDateFormat(API_DISPLAY_FORMAT, Locale.getDefault())
            outputFormat.format(parsedDate)
        } catch (e: Exception) {
            null
        }
    }
}
