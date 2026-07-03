package com.atvantiq.wfms.ui.screens.attendance

import com.atvantiq.wfms.constants.StatusCodes

enum class WorkFilter(val label: String, val statusParam: String?) {
    ALL("All", null),
    PENDING("Pending", "${StatusCodes.OPEN},${StatusCodes.ACCEPTED}"),
    ACTIVE("Active", "${StatusCodes.WIP}"),
    COMPLETED("Completed", "${StatusCodes.COMPLETED}")
}
