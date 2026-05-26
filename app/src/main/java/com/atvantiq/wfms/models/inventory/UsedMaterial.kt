package com.atvantiq.wfms.models.inventory

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


@Parcelize
data class UsedMaterial(
    @SerializedName("material_id")   val materialId: Long,
    @SerializedName("used_quantity") val usedQuantity: Double
) : Parcelable
