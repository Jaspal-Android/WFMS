package com.atvantiq.wfms.models.site.detail


import com.google.gson.annotations.SerializedName

data class SiteDetailResponse(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("data")
    val `data`: SiteDetail?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("success")
    val success: Boolean?
)

data class SiteDetail(
    @SerializedName("id")
    val id: Long?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("project")
    val project: SiteProject?,
    @SerializedName("circle")
    val circle: SiteCircle?
)

data class SiteProject(
    @SerializedName("id")
    val id: Long?,
    @SerializedName("name")
    val name: String?
)

data class SiteCircle(
    @SerializedName("id")
    val id: Long?,
    @SerializedName("code")
    val code: String?,
    @SerializedName("name")
    val name: String?
)
