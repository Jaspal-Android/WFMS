package com.atvantiq.wfms.models.targets

import com.google.gson.annotations.SerializedName

data class MyProjectsResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: MyProjectsData?
)

data class MyProjectsData(
    @SerializedName("month") val month: String?,
    @SerializedName("total_count") val totalCount: Int?,
    @SerializedName("items") val items: List<ProjectBudgetItem>?
)

data class ProjectBudgetItem(
    @SerializedName("project") val project: ProjectInfo?,
    @SerializedName("client") val client: ClientInfo?,
    @SerializedName("circle") val circle: CircleInfo?,
    @SerializedName("status") val status: String?,
    @SerializedName("sites") val sites: TargetSites?,
    @SerializedName("revenue") val revenue: TargetRevenue?
)

data class ProjectInfo(
    @SerializedName("id") val id: Long?,
    @SerializedName("name") val name: String?
)

data class ClientInfo(
    @SerializedName("id") val id: Long?,
    @SerializedName("name") val name: String?
)

data class CircleInfo(
    @SerializedName("id") val id: Long?,
    @SerializedName("code") val code: String?,
    @SerializedName("name") val name: String?
)
