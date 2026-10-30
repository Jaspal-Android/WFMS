package com.atvantiq.wfms.models.empDetail

import com.atvantiq.wfms.constants.FeatureAccess

/**
 * Whether [feature] is granted with any of [levels]. `Full Access` implies every level. Names are
 * trimmed and compared case-insensitively; missing fields (Gson can leave them null) grant nothing.
 */
fun List<Permission>?.allows(feature: String, vararg levels: String): Boolean =
    orEmpty().any { permission ->
        permission.featureName.orEmpty().trim().equals(feature, ignoreCase = true) &&
            permission.accessLevels.orEmpty().any { level ->
                val access = level.access.orEmpty().trim()
                access.equals(FeatureAccess.FULL_ACCESS, ignoreCase = true) ||
                    levels.any { access.equals(it, ignoreCase = true) }
            }
    }
