package com.atvantiq.wfms.models.reimbursement.review

import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

/**
 * Reads `data` of `GET /claim/all` the lenient way the spec asks for (8.1): an array, or an object
 * holding `records` or `claims`; ids and employee fields under either name; `status` as a string
 * or an object with `code`, `name` or `status`. A field of the wrong type reads as null.
 */
class ClaimReviewPageDeserializer : JsonDeserializer<ClaimReviewPage?> {

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): ClaimReviewPage? {
        if (json == null || json.isJsonNull) return null
        val (items, total) = when {
            json.isJsonArray -> json.asJsonArray to null
            json.isJsonObject -> {
                val data = json.asJsonObject
                (data.array(RECORDS) ?: data.array(CLAIMS) ?: JsonArray()) to data.int(TOTAL_RECORDS)
            }
            else -> JsonArray() to null
        }
        val records = items.filter { it.isJsonObject }.map { record(it.asJsonObject) }
        return ClaimReviewPage(records, total)
    }

    private fun record(json: JsonObject): ClaimReviewRecord {
        val employee = json.obj(EMPLOYEE)
        return ClaimReviewRecord(
            claimId = json.long(CLAIM_ID) ?: json.long(ID),
            claimNumber = json.string(CLAIM_NUMBER),
            date = json.string(DATE),
            createdAt = json.string(CREATED_AT),
            employeeId = employee?.long(EMPLOYEE_ID) ?: employee?.long(ID),
            employeeName = employee?.string(EMPLOYEE_NAME) ?: employee?.string(NAME),
            employeeCode = employee?.string(EMPLOYEE_CODE) ?: employee?.string(CODE),
            expenseCategory = json.string(EXPENSE_CATEGORY),
            totalAmount = json.double(TOTAL_AMOUNT),
            latestApprovedAmount = json.double(LATEST_APPROVED_AMOUNT),
            status = ClaimStatusDeserializer.statusText(json.get(STATUS))
        )
    }

    private fun JsonObject.primitive(key: String) = get(key)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive
    private fun JsonObject.string(key: String): String? = primitive(key)?.asString
    private fun JsonObject.long(key: String): Long? = primitive(key)?.let { if (it.isNumber) it.asLong else it.asString.toLongOrNull() }
    private fun JsonObject.int(key: String): Int? = primitive(key)?.let { if (it.isNumber) it.asInt else it.asString.toIntOrNull() }
    private fun JsonObject.double(key: String): Double? = primitive(key)?.let { if (it.isNumber) it.asDouble else it.asString.toDoubleOrNull() }
    private fun JsonObject.obj(key: String): JsonObject? = get(key)?.takeIf { it.isJsonObject }?.asJsonObject
    private fun JsonObject.array(key: String): JsonArray? = get(key)?.takeIf { it.isJsonArray }?.asJsonArray

    private companion object {
        const val RECORDS = "records"
        const val CLAIMS = "claims"
        const val TOTAL_RECORDS = "total_records"
        const val CLAIM_ID = "claim_id"
        const val ID = "id"
        const val CLAIM_NUMBER = "claim_number"
        const val DATE = "date"
        const val CREATED_AT = "created_at"
        const val EMPLOYEE = "employee"
        const val EMPLOYEE_ID = "employee_id"
        const val EMPLOYEE_NAME = "employee_name"
        const val EMPLOYEE_CODE = "employee_code"
        const val NAME = "name"
        const val CODE = "code"
        const val EXPENSE_CATEGORY = "expense_category"
        const val TOTAL_AMOUNT = "total_amount"
        const val LATEST_APPROVED_AMOUNT = "latest_approved_amount"
        const val STATUS = "status"
    }
}
