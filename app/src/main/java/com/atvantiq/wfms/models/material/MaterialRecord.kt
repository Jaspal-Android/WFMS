package com.atvantiq.wfms.models.material

data class MaterialRecord(
    val date: String,
    val site: String,
    val material: String,
    val unit: String,
    val total: Float,
    val consumed: Float
)