package com.atvantiq.wfms.data.tracking

import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.data.repository.tracking.ITrackingRepo
import com.atvantiq.wfms.utils.isUnauthorized
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** How an upload of the queued locations ended. */
sealed interface UploadOutcome {
    /** Everything queued was sent. */
    data object Complete : UploadOutcome

    /** Stopped at the first failure; the rest stays queued for the next attempt. */
    data class Paused(val cause: Throwable) : UploadOutcome

    /** The session is gone (HTTP 401, or HTTP 200 carrying code 401): retrying cannot succeed. */
    data object SessionExpired : UploadOutcome
}

/**
 * Sends the queued locations in order and removes exactly the ones that arrived, so a failure,
 * an expired session or a cancelled coroutine never loses or repeats a point.
 */
class QueueUploader @Inject constructor(
    private val trackingRepo: ITrackingRepo,
    private val queue: LocationEventQueue
) {

    suspend fun flush(): UploadOutcome {
        var sent = 0
        try {
            for (event in queue.peekAll()) {
                val answer = try {
                    trackingRepo.sendLocation(event.toUploadParams())
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    return if (failure.isUnauthorized()) UploadOutcome.SessionExpired else UploadOutcome.Paused(failure)
                }
                if (answer.code == ValConstants.UNAUTHORIZED_CODE) return UploadOutcome.SessionExpired
                sent++
            }
            return UploadOutcome.Complete
        } finally {
            // Also runs when the coroutine is cancelled mid-upload, so sent points are not re-sent.
            if (sent > 0) withContext(NonCancellable) { queue.removeSynced(sent) }
        }
    }
}
