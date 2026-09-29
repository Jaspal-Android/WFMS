package com.atvantiq.wfms.ui.screens.reimbursement.createClaim

import com.atvantiq.wfms.models.reimbursement.detail.ClaimData
import com.atvantiq.wfms.models.site.detail.SiteDetail
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Edit pre-fills the create-claim form from GET claim/{claim_id}.
 * Fixtures are trimmed copies of real dev responses.
 */
class ClaimEditMapperTest {

    private val gson = Gson()

    private fun claim(json: String): ClaimData = gson.fromJson(json, ClaimData::class.java)

    /** Trimmed real GET site/{id} response data. */
    private val site: SiteDetail = gson.fromJson(
        """{"id":754913234180,"name":"South Chandigahr",
            "circle":{"id":725478751706,"code":"CHD","name":"Chandigarh"},
            "project":{"id":163264443387,"name":"shivam testing project"}}""",
        SiteDetail::class.java
    )

    private fun edit(json: String) = ClaimEditMapper.toEditableClaim(claim(json), site)

    private val singleSiteLocal = """
        {"id":450067071202,"date":"2026-09-23","claim_purpose":null,"claim_category":"local",
         "claim_type":"single_site","travelling_from":"CHD","travelling_to":"MOHALI","remarks":null,
         "status":"Submitted","total_amount":700.0,
         "sites":[{"site_id":754913234180,"site_name":"South Chandigahr","work_site_id":436860176310,
           "purchase_order_ids":[434870217788],"amount_site":700.0,"expenses":[
           {"expense_id":552769254885,"expense_type":"travel","claimed_amount":200.0,"travel_mode":"Auto / Local Taxi",
            "start_location":"CHD","end_location":"MOHALI","travelling_with":[],"receipts":[]},
           {"expense_id":580234272092,"expense_type":"travel","claimed_amount":200.0,"travel_mode":"Bus",
            "start_location":"MOHALI","end_location":"LDH","travelling_with":[{"id":148425113430,"name":"Ravi"}],"receipts":[]},
           {"expense_id":215809594585,"expense_type":"DA","claimed_amount":300.0,"travelling_with":[],"receipts":[]}
         ]}]}
    """.trimIndent()

    @Test
    fun `locks header fields exactly as returned`() {
        val header = edit(singleSiteLocal)!!.header

        assertEquals(450067071202, header.claimId)
        assertEquals("2026-09-23", header.date)
        assertEquals("single_site", header.claimType)
        assertEquals("local", header.expenseCategory)
        assertEquals("", header.purpose)
        assertEquals(listOf(LockedSite(754913234180, 436860176310, listOf(434870217788))), header.sites)
        assertEquals(listOf("South Chandigahr"), header.siteNames)
        assertEquals(163264443387, header.projectId)
        assertEquals(725478751706, header.circleId)
        assertEquals("CHD", header.circleCode)
    }

    @Test
    fun `claim cannot be edited without its site's project and circle`() {
        val noProject = gson.fromJson("""{"id":10,"circle":{"id":5}}""", SiteDetail::class.java)
        val noCircle = gson.fromJson("""{"id":10,"project":{"id":5}}""", SiteDetail::class.java)

        assertNull(ClaimEditMapper.toEditableClaim(claim(singleSiteLocal), null))
        assertNull(ClaimEditMapper.toEditableClaim(claim(singleSiteLocal), noProject))
        assertNull(ClaimEditMapper.toEditableClaim(claim(singleSiteLocal), noCircle))
    }

    @Test
    fun `maps travel and DA entries with mode and whole-number amounts`() {
        val editable = edit(singleSiteLocal)!!

        assertEquals(2, editable.travel.size)
        assertEquals("AUTO_LOCAL_TAXI", editable.travel[0].mode?.value)
        assertEquals("200", editable.travel[0].amount)
        assertEquals("BUS", editable.travel[1].mode?.value)
        assertEquals(1, editable.da.size)
        assertEquals("300", editable.da[0].amount)
        assertTrue(editable.hotel.isEmpty())
        assertTrue(editable.other.isEmpty())
    }

    @Test
    fun `each trip keeps its own route and companions`() {
        val travel = edit(singleSiteLocal)!!.travel

        assertEquals("CHD" to "MOHALI", travel[0].from to travel[0].to)
        assertEquals("MOHALI" to "LDH", travel[1].from to travel[1].to)
        assertTrue(travel[0].travelingWith.isNullOrEmpty())
        assertEquals(148425113430, travel[1].travelingWith?.single()?.id)
        assertEquals("Ravi", travel[1].travelingWith?.single()?.name)
    }

    @Test
    fun `auto-fetched KM data is carried over unchanged`() {
        val json = """
            {"id":1,"sites":[{"site_id":10,"expenses":[
              {"expense_id":1,"expense_type":"travel","claimed_amount":20.0,"travel_mode":"Bike (Auto Fetch KM)",
               "start_location":"Check-in location","end_location":"South Chandigahr",
               "distance_km":5.5,"distance_source":"AUTO",
               "trip_refs":[{"start_at":"2026-09-23T11:58:51+00:00","end_at":"2026-09-23T13:11:50+00:00"}]}]}]}
        """.trimIndent()

        val trip = edit(json)!!.travel.single()

        assertEquals("BIKE_AUTO_KM", trip.mode?.value)
        assertEquals(5.5, trip.distanceKm!!, 0.0)
        assertEquals("AUTO", trip.distanceSource)
        assertEquals("2026-09-23T11:58:51+00:00", trip.tripRefs?.single()?.startAt)
    }

    @Test
    fun `expense repeated across sites of a multi-site claim is kept once`() {
        val json = """
            {"id":1,"claim_type":"multiple_site","claim_category":"outstation","claim_purpose":"MRN",
             "sites":[
               {"site_id":10,"site_name":"A","expenses":[
                 {"expense_id":7,"expense_type":"hotel","claimed_amount":1000.0,"amount_per_site":500.0},
                 {"expense_id":8,"expense_type":"other","claimed_amount":99.5,"other_expense_detail":"Parking"}]},
               {"site_id":11,"site_name":"B","expenses":[
                 {"expense_id":7,"expense_type":"hotel","claimed_amount":1000.0,"amount_per_site":500.0}]}
             ]}
        """.trimIndent()

        val editable = edit(json)!!

        assertEquals(listOf(10L, 11L), editable.header.sites.map { it.id })
        assertEquals("MRN", editable.header.purpose)
        assertEquals(1, editable.hotel.size)
        assertEquals("1000", editable.hotel[0].amount)
        assertEquals("Parking", editable.other[0].category)
        assertEquals("99.5", editable.other[0].amount)
    }

    @Test
    fun `unknown travel mode label is kept as-is`() {
        val json = """
            {"id":1,"sites":[{"site_id":10,"expenses":[
              {"expense_id":1,"expense_type":"travel","claimed_amount":50.0,"travel_mode":"Ferry"}]}]}
        """.trimIndent()

        val mode = edit(json)!!.travel.single().mode

        assertEquals("Ferry", mode?.label)
    }

    @Test
    fun `claim without id cannot be edited`() {
        assertNull(edit("""{"sites":[]}"""))
    }

    @Test
    fun `formatAmount strips trailing zeros only`() {
        assertEquals("200", ClaimEditMapper.formatAmount(200.0))
        assertEquals("200.5", ClaimEditMapper.formatAmount(200.5))
        assertEquals("0", ClaimEditMapper.formatAmount(null))
    }
}
