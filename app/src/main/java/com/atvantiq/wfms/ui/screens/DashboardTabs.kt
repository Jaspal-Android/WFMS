package com.atvantiq.wfms.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.atvantiq.wfms.R
import com.atvantiq.wfms.constants.AppRole
import com.atvantiq.wfms.constants.FeatureAccess
import com.atvantiq.wfms.models.empDetail.Permission
import com.atvantiq.wfms.models.empDetail.allows

/** A bottom-navigation tab. [destinationId] is both the navigation graph destination and the menu item id. */
enum class DashboardTab(
    @IdRes val destinationId: Int,
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int
) {
    DASHBOARD(R.id.nav_dashboard, R.string.menu_dashboard, R.drawable.ic_tab_home),
    WORK(R.id.nav_attendance, R.string.tab_work, R.drawable.ic_tab_work),
    CLAIMS(R.id.nav_reimbursement, R.string.tab_claims, R.drawable.ic_tab_claims),
    SITES(R.id.nav_sites, R.string.sites, R.drawable.ic_tab_sites),
    APPROVALS(R.id.nav_approvals, R.string.approvals, R.drawable.ic_tab_approvals),
    MORE(R.id.nav_more, R.string.tab_more, R.drawable.ic_tab_more)
}

/** Which tabs a role sees. Kept free of Android so it is unit tested. */
object DashboardTabs {

    /**
     * Employees: Dashboard · Work · Claims · More. Everyone else: Dashboard · Sites · Approvals ·
     * More, where Sites needs the `Site` permission (the backend answers 403 without it) and
     * Approvals needs a PM, OPS or admin role.
     */
    fun forRole(role: AppRole, permissions: List<Permission>?): List<DashboardTab> = buildList {
        add(DashboardTab.DASHBOARD)
        if (role == AppRole.EMPLOYEE) {
            add(DashboardTab.WORK)
            add(DashboardTab.CLAIMS)
        } else {
            if (permissions.allows(FeatureAccess.FEATURE_SITE, FeatureAccess.VIEW)) add(DashboardTab.SITES)
            if (role.canApprove) add(DashboardTab.APPROVALS)
        }
        add(DashboardTab.MORE)
    }

    /**
     * Where "Enter Work Details" goes after a check-out with no work: Work for employees, otherwise
     * Approvals, or Sites for those who can't approve. Null when the role has none of them.
     */
    fun workEntryTab(tabs: List<DashboardTab>): DashboardTab? =
        listOf(DashboardTab.WORK, DashboardTab.APPROVALS, DashboardTab.SITES).firstOrNull { it in tabs }
}
