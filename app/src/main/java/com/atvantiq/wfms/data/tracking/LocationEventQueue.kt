package com.atvantiq.wfms.data.tracking

import com.atvantiq.wfms.data.prefs.PrefKeys
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import javax.inject.Inject
import javax.inject.Singleton

// Persisted as JSON, so the names must not depend on what R8 renames the fields to.
data class QueuedLocationEvent(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("recordedAtMillis") val recordedAtMillis: Long,
    @SerializedName("accuracyMeters") val accuracyMeters: Float?
)

@Singleton
class LocationEventQueue @Inject constructor(
    private val prefMain: SecurePrefMain
) {
    private val gson = Gson()

    @Synchronized
    fun enqueue(event: QueuedLocationEvent) {
        val updated = (readQueue() + event).takeLast(MAX_QUEUE_SIZE)
        writeQueue(updated)
    }

    @Synchronized
    fun peekAll(): List<QueuedLocationEvent> = readQueue()

    @Synchronized
    fun removeSynced(count: Int) {
        if (count <= 0) return
        writeQueue(readQueue().drop(count))
    }

    @Synchronized
    fun clear() {
        prefMain.delete(PrefKeys.LOCATION_EVENT_QUEUE)
    }

    private fun readQueue(): List<QueuedLocationEvent> {
        val raw = prefMain.get(PrefKeys.LOCATION_EVENT_QUEUE, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            // Entries written by releases that predate @SerializedName carry R8-obfuscated keys.
            // Gson would decode those as (0.0, 0.0), which must never be uploaded as a location,
            // so anything without the real keys is dropped.
            JsonParser.parseString(raw).asJsonArray
                .map { it.asJsonObject }
                .filter { it.has("latitude") && it.has("longitude") && it.has("recordedAtMillis") }
                .map { gson.fromJson(it, QueuedLocationEvent::class.java) }
        }.getOrElse {
            emptyList()
        }
    }

    private fun writeQueue(queue: List<QueuedLocationEvent>) {
        if (queue.isEmpty()) {
            prefMain.delete(PrefKeys.LOCATION_EVENT_QUEUE)
        } else {
            prefMain.put(PrefKeys.LOCATION_EVENT_QUEUE, gson.toJson(queue))
        }
    }

    companion object {
        private const val MAX_QUEUE_SIZE = 500
    }
}
