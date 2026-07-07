package com.atvantiq.wfms.data.tracking

import com.atvantiq.wfms.data.prefs.PrefKeys
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

data class QueuedLocationEvent(
    val latitude: Double,
    val longitude: Double,
    val recordedAtMillis: Long,
    val accuracyMeters: Float?
)

@Singleton
class LocationEventQueue @Inject constructor(
    private val prefMain: SecurePrefMain
) {
    private val gson = Gson()
    private val queueType = object : TypeToken<List<QueuedLocationEvent>>() {}.type

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
            gson.fromJson<List<QueuedLocationEvent>>(raw, queueType).orEmpty()
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
