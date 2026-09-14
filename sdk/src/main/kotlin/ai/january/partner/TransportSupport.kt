package ai.january.partner

import ai.january.partner.transport.infrastructure.Serializer
import java.io.IOException
import java.net.SocketTimeoutException
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.CancellationException
import retrofit2.Response

/**
 * Raised inside the OkHttp interceptor when the client token provider fails.
 * OkHttp only lets interceptors throw IOException, so the real failure travels
 * as the cause and is unwrapped by [executeApiCall] and [executeEmptyApiCall].
 */
internal class ClientTokenUnavailableException(
    override val cause: Throwable,
) : IOException(cause.message, cause) {
    fun asJanuaryException(): JanuaryException = when (cause) {
        is JanuaryException -> cause
        else -> JanuaryException(
            ErrorCategory.AUTHENTICATION,
            "The app could not obtain a January client token: ${cause.message ?: cause::class.java.simpleName}",
            cause = cause,
        )
    }
}

internal inline fun <reified Source, reified Target> bridgeModel(value: Source): Target {
    val moshi = Serializer.moshiBuilder.build()
    val sourceAdapter = moshi.adapter(Source::class.java)
    val targetAdapter = moshi.adapter(Target::class.java)
    return requireNotNull(targetAdapter.fromJson(sourceAdapter.toJson(value))) {
        "The January API model adapter returned an empty value."
    }
}

internal suspend fun <Transport, Public> executeApiCall(
    operation: suspend () -> Response<Transport>,
    transform: (Transport) -> Public,
): Public {
    try {
        val response = operation()
        if (!response.isSuccessful) {
            val details = runCatching {
                response.errorBody()?.string()?.let { Serializer.moshiBuilder.build().adapter(Map::class.java).fromJson(it) }
            }.getOrNull()
            throw JanuaryException(
                category = categoryForStatus(response.code()),
                message = details?.get("message") as? String ?: "The January API returned HTTP ${response.code()}.",
                httpStatus = response.code(),
                cause = null,
                code = details?.get("code") as? String,
                requestId = details?.get("request_id") as? String ?: response.headers()["x-request-id"],
            )
        }
        return transform(
            response.body() ?: throw JanuaryException(
                ErrorCategory.DECODING,
                "The January API returned an empty response.",
            ),
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: JanuaryException) {
        throw error
    } catch (error: ClientTokenUnavailableException) {
        throw error.asJanuaryException()
    } catch (error: SocketTimeoutException) {
        throw JanuaryException(ErrorCategory.TIMEOUT, "The request to the January API timed out.", cause = error)
    } catch (error: JsonDataException) {
        throw JanuaryException(ErrorCategory.DECODING, "The January API returned an unreadable response.", cause = error)
    } catch (error: JsonEncodingException) {
        throw JanuaryException(ErrorCategory.DECODING, "The January API returned an unreadable response.", cause = error)
    } catch (error: IOException) {
        throw JanuaryException(ErrorCategory.TRANSPORT, "The request to the January API failed.", cause = error)
    }
}

internal suspend fun executeEmptyApiCall(operation: suspend () -> Response<Unit>) {
    try {
        val response = operation()
        if (!response.isSuccessful) {
            val details = runCatching {
                response.errorBody()?.string()?.let { Serializer.moshiBuilder.build().adapter(Map::class.java).fromJson(it) }
            }.getOrNull()
            throw JanuaryException(
                categoryForStatus(response.code()),
                details?.get("message") as? String ?: "The January API returned HTTP ${response.code()}.",
                response.code(),
                cause = null,
                code = details?.get("code") as? String,
                requestId = details?.get("request_id") as? String ?: response.headers()["x-request-id"],
            )
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: JanuaryException) {
        throw error
    } catch (error: ClientTokenUnavailableException) {
        throw error.asJanuaryException()
    } catch (error: SocketTimeoutException) {
        throw JanuaryException(ErrorCategory.TIMEOUT, "The request to the January API timed out.", cause = error)
    } catch (error: IOException) {
        throw JanuaryException(ErrorCategory.TRANSPORT, "The request to the January API failed.", cause = error)
    }
}

internal fun categoryForStatus(status: Int): ErrorCategory = when (status) {
    400, 422 -> ErrorCategory.VALIDATION
    401 -> ErrorCategory.AUTHENTICATION
    403 -> ErrorCategory.AUTHORIZATION
    404 -> ErrorCategory.NOT_FOUND
    429 -> ErrorCategory.RATE_LIMITED
    504 -> ErrorCategory.TIMEOUT
    in 500..599 -> ErrorCategory.SERVER
    else -> ErrorCategory.TRANSPORT
}
