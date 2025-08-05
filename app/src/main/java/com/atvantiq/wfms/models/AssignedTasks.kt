package com.atvantiq.wfms.models

data class AssignedTasks(
    val id: String,
    val project: String,
    val site: String,
    val task: String,
    val status: String,
)
