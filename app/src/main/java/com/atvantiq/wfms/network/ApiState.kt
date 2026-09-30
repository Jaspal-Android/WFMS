package com.atvantiq.wfms.network

data class ApiState<out T>(val status: Status, val response: T?, val throwable:Throwable?) {

    private var consumed = false

    /**
     * Returns true only the first time it is called for this state.
     *
     * LiveData hands its last value to every new observer (view or activity recreation), so
     * one-shot UI work such as dialogs and tracking changes must be gated on this. Gating on the
     * lifecycle being RESUMED instead drops results that arrive while the screen is only STARTED
     * (permission dialog, split screen) and LiveData never delivers them again.
     */
    fun consumeOnce(): Boolean {
        if (consumed) return false
        consumed = true
        return true
    }

    companion object {
        fun <T> success(response: T?): ApiState<T> {
            return ApiState(Status.SUCCESS, response, null)
        }
        fun <T> error(throwable:Throwable): ApiState<T> {
            return ApiState(Status.ERROR, null, throwable)
        }
        fun <T> loading(): ApiState<T> {
            return ApiState(Status.LOADING, null, null)
        }
    }
}