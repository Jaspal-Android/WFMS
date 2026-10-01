package com.atvantiq.wfms.ui.screens.attendance.addSignInActivity

import com.atvantiq.wfms.models.activity.ActivityData

/** One selectable activity, tied to the type it belongs to. */
data class TypeActivityOption(
    val typeId: Long,
    val typeName: String,
    val activity: ActivityData
)

/**
 * Activities are defined per type, so the ones a user picks must stay attached to the type they
 * were picked for. This holds the available activities and the picks for every selected type.
 */
class TypeActivitySelection {

    private val availableByType = LinkedHashMap<Long, List<ActivityData>>()
    private val selectedByType = LinkedHashMap<Long, Set<Long>>()

    fun clear() {
        availableByType.clear()
        selectedByType.clear()
    }

    /** Forgets everything about types that are no longer selected; the others keep their picks. */
    fun retainTypes(typeIds: Set<Long>) {
        availableByType.keys.retainAll(typeIds)
        selectedByType.keys.retainAll(typeIds)
    }

    fun hasActivities(typeId: Long): Boolean = availableByType[typeId].orEmpty().isNotEmpty()

    /** Stores the activities of a type and drops picks the server no longer offers. */
    fun setAvailable(typeId: Long, activities: List<ActivityData>) {
        availableByType[typeId] = activities
        val offered = activities.map { it.id }.toSet()
        selectedByType[typeId]?.let { picked -> selectedByType[typeId] = picked.intersect(offered) }
    }

    /** All activities that can be picked for [types], in type order. */
    fun options(types: List<Pair<Long, String>>): List<TypeActivityOption> =
        types.flatMap { (typeId, typeName) ->
            availableByType[typeId].orEmpty().map { TypeActivityOption(typeId, typeName, it) }
        }

    fun selectedOptions(types: List<Pair<Long, String>>): Set<TypeActivityOption> =
        options(types).filter { it.activity.id in selectedIds(it.typeId) }.toSet()

    /** Replaces the picks of every type with [picked]. */
    fun select(picked: Set<TypeActivityOption>) {
        selectedByType.clear()
        picked.groupBy { it.typeId }.forEach { (typeId, options) ->
            selectedByType[typeId] = options.map { it.activity.id }.toCollection(LinkedHashSet())
        }
    }

    fun selectedIds(typeId: Long): Set<Long> = selectedByType[typeId].orEmpty()

    /** True when every one of [typeIds] has at least one activity picked. */
    fun coversAll(typeIds: Collection<Long>): Boolean =
        typeIds.isNotEmpty() && typeIds.all { selectedIds(it).isNotEmpty() }
}
