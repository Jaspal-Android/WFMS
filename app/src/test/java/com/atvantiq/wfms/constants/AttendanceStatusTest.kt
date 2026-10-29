package com.atvantiq.wfms.constants

import com.atvantiq.wfms.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AttendanceStatusTest {

    @Test
    fun `every known status has its own spoken label`() {
        val labels = mapOf(
            AttendanceStatus.PRESENT to R.string.present,
            AttendanceStatus.ABSENT to R.string.absent,
            AttendanceStatus.LEAVE to R.string.leave,
            AttendanceStatus.IDLE to R.string.idle,
            AttendanceStatus.HOLIDAY to R.string.holiday,
            AttendanceStatus.WORK_OFF to R.string.work_off,
            AttendanceStatus.ABSENT_NA to R.string.absent_system_generated,
            AttendanceStatus.INCOMPLETE to R.string.incomplete,
            AttendanceStatus.NO_ACTION to R.string.no_action
        )

        labels.forEach { (status, expected) -> assertEquals(status, expected, AttendanceStatus.labelRes(status)) }
        assertEquals("labels must be distinct", labels.size, labels.values.toSet().size)
    }

    @Test
    fun `an unknown or unmarked status reads as not marked, never as another status`() {
        assertEquals(R.string.status_not_marked, AttendanceStatus.labelRes(AttendanceStatus.UNKNOWN))
        assertEquals(R.string.status_not_marked, AttendanceStatus.labelRes(""))
        assertNotEquals(R.string.present, AttendanceStatus.labelRes("something-new"))
    }
}
