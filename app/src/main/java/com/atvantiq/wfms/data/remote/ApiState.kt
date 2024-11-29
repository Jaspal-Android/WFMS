import com.atvantiq.wfms.data.remote.Status

data class ApiState<out T>(val status: Status, val response: T?, val throwable:Throwable?) {
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