package com.atvantiq.wfms.ui.screens.admin.ui.site.addSite

import androidx.annotation.StringRes
import com.atvantiq.wfms.R

/** Add Site's checks, in the spec's order (4); the first that fails is shown under the form. */
object AddSiteValidation {

    private const val MAX_LATITUDE = 90.0
    private const val MAX_LONGITUDE = 180.0

    @StringRes
    fun firstError(
        hasClient: Boolean,
        hasProject: Boolean,
        hasCircle: Boolean,
        siteId: String?,
        name: String?,
        address: String?,
        latitude: String?,
        longitude: String?
    ): Int? = when {
        !hasClient -> R.string.add_site_select_client
        !hasProject -> R.string.add_site_select_project
        !hasCircle -> R.string.add_site_select_circle
        siteId.isNullOrBlank() -> R.string.add_site_enter_site_id
        name.isNullOrBlank() -> R.string.add_site_enter_name
        address.isNullOrBlank() -> R.string.add_site_enter_address
        !isOptionalCoordinate(latitude, MAX_LATITUDE) -> R.string.add_site_invalid_latitude
        !isOptionalCoordinate(longitude, MAX_LONGITUDE) -> R.string.add_site_invalid_longitude
        else -> null
    }

    /** Empty, or a number within ±[limit]. */
    private fun isOptionalCoordinate(value: String?, limit: Double): Boolean {
        val text = value?.trim().orEmpty()
        if (text.isEmpty()) return true
        val number = text.toDoubleOrNull() ?: return false
        return number in -limit..limit
    }
}
