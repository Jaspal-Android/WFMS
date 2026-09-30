package com.atvantiq.wfms.constants

import androidx.annotation.StringRes
import com.atvantiq.wfms.R

object AttendanceStatus {
    const val NO_ACTION ="NO_ACTION"
    const val PRESENT = "P"
    const val ABSENT = "A"
    const val LEAVE = "L"
    const val IDLE = "IDLE"
    const val HOLIDAY = "H"
    const val WORK_OFF = "WO"
    const val UNKNOWN = "UNKNOWN"
    const val ABSENT_NA = "ABSENT_NA"
    const val INCOMPLETE = "INCOMPLETE"

    /** The word a screen reader should say for a calendar day, since the color alone says nothing. */
    @StringRes
    fun labelRes(status: String): Int = when (status) {
        PRESENT -> R.string.present
        ABSENT -> R.string.absent
        LEAVE -> R.string.leave
        IDLE -> R.string.idle
        HOLIDAY -> R.string.holiday
        WORK_OFF -> R.string.work_off
        ABSENT_NA -> R.string.absent_system_generated
        INCOMPLETE -> R.string.incomplete
        NO_ACTION -> R.string.no_action
        else -> R.string.status_not_marked
    }
}


/*
"data": [
{
    "code": 0,
    "name": "NO_ACTION"
},
{
    "code": 1,
    "name": "P"
},
{
    "code": 2,
    "name": "A"
},
{
    "code": 3,
    "name": "L"
},
{
    "code": 4,
    "name": "IDLE"
},
{
    "code": 5,
    "name": "H"
},
{
    "code": 6,
    "name": "WO"
},
{
    "code": 7,
    "name": "ABSENT_NA"
},{
    "code": 8,
    "name": "INCOMPLETE"
}
],*/
