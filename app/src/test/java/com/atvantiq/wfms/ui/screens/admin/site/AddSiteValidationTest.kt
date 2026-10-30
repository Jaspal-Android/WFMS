package com.atvantiq.wfms.ui.screens.admin.site

import com.atvantiq.wfms.R
import com.atvantiq.wfms.ui.screens.admin.ui.site.addSite.AddSiteValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Add Site's checks, in the spec's order (4). */
class AddSiteValidationTest {

    private fun error(
        client: Boolean = true, project: Boolean = true, circle: Boolean = true,
        siteId: String? = "CHD-600", name: String? = "North Gate", address: String? = "Sector 17",
        latitude: String? = "", longitude: String? = ""
    ) = AddSiteValidation.firstError(client, project, circle, siteId, name, address, latitude, longitude)

    @Test
    fun `a complete form passes, with or without coordinates`() {
        assertNull(error())
        assertNull(error(latitude = "30.7415", longitude = "76.7823"))
        assertNull(error(latitude = "-90", longitude = "180"))
    }

    @Test
    fun `checks run in order and report the first failure`() {
        assertEquals(R.string.add_site_select_client, error(client = false, project = false, siteId = ""))
        assertEquals(R.string.add_site_select_project, error(project = false, circle = false))
        assertEquals(R.string.add_site_select_circle, error(circle = false))
        assertEquals(R.string.add_site_enter_site_id, error(siteId = "  "))
        assertEquals(R.string.add_site_enter_name, error(name = ""))
        assertEquals(R.string.add_site_enter_address, error(address = null))
    }

    @Test
    fun `coordinates must be numbers within range when given`() {
        assertEquals(R.string.add_site_invalid_latitude, error(latitude = "90.1"))
        assertEquals(R.string.add_site_invalid_latitude, error(latitude = "north"))
        assertEquals(R.string.add_site_invalid_longitude, error(longitude = "-180.5"))
    }
}
