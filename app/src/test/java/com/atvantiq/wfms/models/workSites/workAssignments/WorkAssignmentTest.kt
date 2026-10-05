package com.atvantiq.wfms.models.workSites.workAssignments

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkAssignmentTest {

    /** A record as `GET /work/all` sends it, with the fields the screen ignores included. */
    private val json = """
        {"code":200,"message":"Work assignments fetched successfully","success":true,
         "data":{"page":1,"page_size":20,"total_count":3,"total_records":3,"total_pages":1,"total_tasks":3,
                 "open":1,"in_progress":1,"completed":1,"columns":["employee"],"status_map":{"3":"WIP"},
                 "records":[{"id":883195058957,
                   "employee":{"id":505063676579,"employee_code":"kamal23","name":"kamal sharma",
                               "check_in":"2026-09-23T11:58:51.961733Z","check_out":"2026-09-23T15:42:13.500937Z"},
                   "po":{"id":135877959622,"po_number":"S0-5013"},
                   "project":{"id":163264443387,"client":299257598498,"name":"shivam testing project"},
                   "site":{"id":616366488359,"site_id":"CHD-100","name":"South Chandigahr","work_site_id":216657125534},
                   "type":[{"id":548086145405,"name":"Site Survey","status":{"code":3,"label":"WIP"}}],
                   "circle":{"id":725478751706,"code":"CHD","name":"Chandigarh"},
                   "status":{"code":3,"label":"WIP"},"assigned_by":{"id":116307872557,"name":"Admin3"},
                   "visit_no":1,"progress":{"total":1,"completed":0,"percentage":0.0},
                   "created_at":"2026-09-23T13:19:29.770067Z","updated_at":"2026-09-23T13:19:29.770067Z"}]}}
    """.trimIndent()

    private val response = Gson().fromJson(json, WorkAssignmentsResponse::class.java)
    private val record get() = response.data!!.records!!.single()

    @Test
    fun `the page and a record are read as the API sends them`() {
        assertEquals(200, response.code)
        assertEquals(3, response.data?.totalRecords)
        assertEquals(883195058957L, record.id)
        assertEquals("kamal23", record.employee?.code)
        assertEquals(505063676579L, record.employee?.id)
        assertEquals(216657125534L, record.site?.workSiteId)
        assertEquals("South Chandigahr", record.site?.name)
        assertEquals("shivam testing project", record.project?.name)
        assertEquals("Chandigarh", record.circle?.name)
        assertEquals(3, record.status?.code)
        assertEquals(1, record.visitNo)
        assertEquals(0, record.progress?.completed)
        assertEquals(1, record.progress?.total)
    }

    @Test
    fun `the work date is the check-in day`() {
        assertEquals("2026-09-23", record.workDate)
    }

    @Test
    fun `without a check-in the work date is the day it was assigned`() {
        val notCheckedIn = record.copy(employee = record.employee?.copy(checkIn = null), createdAt = "2026-09-24T01:00:00Z")
        assertEquals("2026-09-24", notCheckedIn.workDate)
    }

    @Test
    fun `no usable timestamp gives no work date`() {
        assertNull(record.copy(employee = null, createdAt = null).workDate)
        assertNull(record.copy(employee = null, createdAt = "yesterday").workDate)
    }
}
