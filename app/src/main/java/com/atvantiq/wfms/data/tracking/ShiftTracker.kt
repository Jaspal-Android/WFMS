package com.atvantiq.wfms.data.tracking

import com.atvantiq.wfms.data.prefs.PrefKeys
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Running totals of a shift, kept as fixes arrive so the notification never re-reads the trail:
 * every accepted fix is counted; the distance applies the same filters as [TrackMath].
 */
data class TrackTally(
    @SerializedName("locations") val locations: Int = 0,
    @SerializedName("distanceKm") val distanceKm: Double = 0.0,
    @SerializedName("lastCounted") val lastCounted: TrackPoint? = null
) {
    fun add(point: TrackPoint): TrackTally {
        val counted = TrackMath.isCountable(point) &&
            (lastCounted == null || !TrackMath.isJump(lastCounted, point))
        return copy(
            locations = locations + 1,
            distanceKm = if (counted && lastCounted != null) distanceKm + TrackMath.haversineKm(lastCounted, point) else distanceKm,
            lastCounted = if (counted) point else lastCounted
        )
    }
}

/** What the dashboard card and the shift notification show about the current shift. */
data class ShiftState(
    @SerializedName("checkInMillis") val checkInMillis: Long? = null,
    /** Set while a 15-minute pause is running; fixes until then are discarded. */
    @SerializedName("pausedUntilMillis") val pausedUntilMillis: Long? = null,
    @SerializedName("tally") val tally: TrackTally = TrackTally()
) {
    fun isPaused(nowMillis: Long): Boolean = pausedUntilMillis != null && nowMillis < pausedUntilMillis
}

/**
 * The shift's tracking state, shared by the location service, its notification actions and the
 * dashboard card, so Pause and Resume take the same path wherever they are tapped. Persisted, so
 * a pause survives app restarts; cleared on End Day and (with all prefs) on logout.
 *
 * The pause is local only, as on iOS: there is no server call and the gap shows in the trail.
 */
@Singleton
class ShiftTracker @Inject constructor(
    private val prefMain: SecurePrefMain
) {
    private val gson = Gson()

    /** Clock; replaced in tests. */
    internal var now: () -> Long = { System.currentTimeMillis() }

    private val _state = MutableStateFlow(read())
    val state: StateFlow<ShiftState> = _state.asStateFlow()

    /** A shift started (or tracking resumed for it). Totals reset only for a new check-in. */
    @Synchronized
    fun startShift(checkInMillis: Long) {
        val current = _state.value
        if (current.checkInMillis == checkInMillis) return
        write(ShiftState(checkInMillis = checkInMillis))
    }

    /** End Day: the shift's totals and any pause are gone. */
    @Synchronized
    fun endShift() {
        prefMain.delete(PrefKeys.SHIFT_STATE)
        _state.value = ShiftState()
    }

    @Synchronized
    fun pause() {
        write(_state.value.copy(pausedUntilMillis = now() + PAUSE_MILLIS))
    }

    @Synchronized
    fun resume() {
        if (_state.value.pausedUntilMillis == null) return
        write(_state.value.copy(pausedUntilMillis = null))
    }

    /** Clears a pause whose 15 minutes are up. Returns whether tracking is paused now. */
    @Synchronized
    fun refreshPause(): Boolean {
        val current = _state.value
        if (current.pausedUntilMillis != null && !current.isPaused(now())) {
            write(current.copy(pausedUntilMillis = null))
            return false
        }
        return current.isPaused(now())
    }

    /** A new fix. Returns false (discard it) while paused; otherwise adds it to the totals. */
    @Synchronized
    fun recordFix(point: TrackPoint): Boolean {
        if (refreshPause()) return false
        write(_state.value.copy(tally = _state.value.tally.add(point)))
        return true
    }

    private fun write(state: ShiftState) {
        prefMain.put(PrefKeys.SHIFT_STATE, gson.toJson(state))
        _state.value = state
    }

    private fun read(): ShiftState {
        val raw = prefMain.get(PrefKeys.SHIFT_STATE, null)
        return runCatching { gson.fromJson(raw, ShiftState::class.java) }.getOrNull() ?: ShiftState()
    }

    companion object {
        /** Fixed by the business: no picker and no way to stop tracking during a shift. */
        const val PAUSE_MILLIS = 15 * 60 * 1000L
    }
}
