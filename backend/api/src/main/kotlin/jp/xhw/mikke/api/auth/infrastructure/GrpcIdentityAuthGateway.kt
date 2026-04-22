package jp.xhw.mikke.api.auth.infrastructure

import com.google.protobuf.Timestamp
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import jp.xhw.mikke.api.auth.application.*
import jp.xhw.mikke.api.auth.application.AuthSession
import jp.xhw.mikke.api.http.ApiErrorCode
import jp.xhw.mikke.api.http.ApiHttpException
import jp.xhw.mikke.identity.v1.*
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.nio.channels.ClosedChannelException
import java.time.Instant
import java.util.concurrent.TimeoutException

class GrpcIdentityAuthGateway(
    private val channel: ManagedChannel,
    private val stub: IdentityServiceGrpcKt.IdentityServiceCoroutineStub =
        IdentityServiceGrpcKt.IdentityServiceCoroutineStub(
            channel,
        ),
) : IdentityAuthGateway {
    override suspend fun login(command: LoginCommand): LoginResult =
        try {
            stub.loginUser(command.toRequestModel()).toLoginResult()
        } catch (e: Exception) {
            throw e.toApiHttpException()
        }

    override suspend fun register(command: RegisterCommand): RegisterResult =
        try {
            stub.registerUser(command.toRequestModel()).toRegisterResult()
        } catch (e: Exception) {
            throw e.toApiHttpException()
        }

    override fun close() {
        channel.shutdown()
    }

    companion object {
        fun fromEnvironment(): GrpcIdentityAuthGateway {
            val host = System.getenv("IDENTITY_SERVICE_HOST").orEmpty().ifBlank { "localhost" }
            val port = System.getenv("IDENTITY_SERVICE_PORT")?.toIntOrNull() ?: 50051
            val channel =
                ManagedChannelBuilder
                    .forAddress(host, port)
                    .usePlaintext()
                    .build()

            return GrpcIdentityAuthGateway(channel = channel)
        }
    }
}

private fun LoginCommand.toRequestModel(): LoginUserRequest =
    LoginUserRequest
        .newBuilder()
        .setLoginId(loginId)
        .setPassword(password)
        .build()

private fun LoginUserResponse.toLoginResult(): LoginResult =
    LoginResult(
        user =
            AuthenticatedUser(
                id = user.id,
                email = user.email,
                username = user.username,
                displayName = user.displayName,
                status = user.status.toApiStatus(),
                createdAt = user.createdAt.toInstantString(),
                updatedAt = user.updatedAt.toInstantString(),
            ),
        session =
            AuthSession(
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
                accessTokenExpiresAt = session.accessTokenExpiresAt.toInstantString(),
                refreshTokenExpiresAt = session.refreshTokenExpiresAt.toInstantString(),
            ),
    )

private fun RegisterCommand.toRequestModel(): RegisterUserRequest =
    RegisterUserRequest
        .newBuilder()
        .setEmail(email)
        .setUsername(username)
        .setDisplayName(displayName)
        .setPassword(password)
        .build()

private fun RegisterUserResponse.toRegisterResult(): RegisterResult =
    RegisterResult(
        user =
            AuthenticatedUser(
                id = user.id,
                email = user.email,
                username = user.username,
                displayName = user.displayName,
                status = user.status.toApiStatus(),
                createdAt = user.createdAt.toInstantString(),
                updatedAt = user.updatedAt.toInstantString(),
            ),
        session =
            AuthSession(
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
                accessTokenExpiresAt = session.accessTokenExpiresAt.toInstantString(),
                refreshTokenExpiresAt = session.refreshTokenExpiresAt.toInstantString(),
            ),
    )

private fun Timestamp.toInstantString(): String = Instant.ofEpochSecond(seconds, nanos.toLong()).toString()

private fun UserStatus.toApiStatus(): String =
    when (this) {
        UserStatus.USER_STATUS_ACTIVE -> "active"

        UserStatus.USER_STATUS_SUSPENDED -> "suspended"

        UserStatus.USER_STATUS_DEACTIVATED -> "deactivated"

        UserStatus.USER_STATUS_UNSPECIFIED,
        UserStatus.UNRECOGNIZED,
        -> "unspecified"
    }

private fun Exception.toApiHttpException(): ApiHttpException {
    val status = Status.fromThrowable(this)
    val errorCode = status.toApiErrorCode(rootCause())

    return ApiHttpException(
        status = errorCode.status,
        message = status.description ?: defaultUpstreamErrorMessage(errorCode),
    )
}

private fun Status.toApiErrorCode(rootCause: Throwable?): ApiErrorCode =
    when {
        code == Status.Code.INVALID_ARGUMENT -> ApiErrorCode.InvalidRequest
        code == Status.Code.UNAUTHENTICATED -> ApiErrorCode.Unauthenticated
        code == Status.Code.NOT_FOUND -> ApiErrorCode.NotFound
        code == Status.Code.ALREADY_EXISTS -> ApiErrorCode.Conflict
        code == Status.Code.DEADLINE_EXCEEDED -> ApiErrorCode.UpstreamTimeout
        code == Status.Code.UNAVAILABLE -> ApiErrorCode.UpstreamUnavailable
        rootCause is SocketTimeoutException || rootCause is TimeoutException -> ApiErrorCode.UpstreamTimeout
        rootCause is ConnectException || rootCause is ClosedChannelException -> ApiErrorCode.UpstreamUnavailable
        else -> ApiErrorCode.UpstreamFailure
    }

private fun Exception.rootCause(): Throwable {
    var current: Throwable = this
    while (current.cause != null) {
        current = current.cause!!
    }
    return current
}

private fun defaultUpstreamErrorMessage(errorCode: ApiErrorCode): String =
    when (errorCode) {
        ApiErrorCode.UpstreamUnavailable -> "Backend service is unavailable"
        ApiErrorCode.UpstreamTimeout -> "Backend service request timed out"
        ApiErrorCode.UpstreamFailure -> "Backend service request failed"
        ApiErrorCode.InvalidRequest -> "Invalid request"
        ApiErrorCode.Unauthenticated -> "Authentication failed"
        ApiErrorCode.NotFound -> "Resource not found"
        ApiErrorCode.Conflict -> "Conflict"
        ApiErrorCode.InternalError -> "Internal server error"
    }
