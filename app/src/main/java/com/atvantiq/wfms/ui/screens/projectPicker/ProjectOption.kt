package com.atvantiq.wfms.ui.screens.projectPicker

import com.atvantiq.wfms.models.allProjects.Project
import com.atvantiq.wfms.models.project.ManagedProject

/** A project the picker lists: the id sent as `project_id`, its name and its circle's name. */
data class ProjectOption(
    val id: Long,
    val name: String,
    val circleName: String?
)

/** A row without an id can't be booked to, so it is left out. */
fun ManagedProject.toProjectOption(): ProjectOption? =
    id?.let { ProjectOption(it, name.orEmpty(), circle?.name) }

/** `GET /project/all` sends the circles as a list; the first one is the tag. */
fun Project.toProjectOption(): ProjectOption? =
    id?.let { ProjectOption(it, name.orEmpty(), circle?.firstOrNull()?.name) }
