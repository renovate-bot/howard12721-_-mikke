package jp.xhw.mikke.api.auth.infrastructure

import com.google.protobuf.Timestamp
import io.grpc.*
import jp.xhw.mikke.api.auth.application.*
import jp.xhw.mikke.api.http.ApiErrorCode
import jp.xhw.mikke.api.http.ApiHttpException
import jp.xhw.mikke.identity.v1.IdentityServiceGrpcKt
import jp.xhw.mikke.identity.v1.LoginUserRequest
import jp.xhw.mikke.identity.v1.LoginUserResponse
import jp.xhw.mikke.identity.v1.UserStatus
import java.time.Instant

class GrpcIdentityAuthGateway(
    private val channel: ManagedChannel,
    private val stub: IdentityServiceGrpcKt.IdentityServiceCoroutineStub =
        IdentityServiceGrpcKt.IdentityServiceCoroutineStub(
            channel,
        ),
) : IdentityAuthGateway {
    override suspend fun login(command: LoginCommand): LoginResult =
        try {
            stub
                .loginUser(
                    LoginUserRequest
                        .newBuilder()
                        .setLoginId(command.loginId)
                        .setPassword(command.password)
                        .build(),
                ).toLoginResult()
        } catch (e: Exception) {
            when (e) {
                is StatusException,
                is StatusRuntimeException,
                -> throw e.toApiHttpException()

                else -> throw e
            }
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
    val errorCode =
        when (status.code) {
            Status.Code.INVALID_ARGUMENT -> ApiErrorCode.InvalidRequest
            Status.Code.UNAUTHENTICATED -> ApiErrorCode.Unauthenticated
            Status.Code.NOT_FOUND -> ApiErrorCode.NotFound
            Status.Code.ALREADY_EXISTS -> ApiErrorCode.Conflict
            Status.Code.UNAVAILABLE -> ApiErrorCode.UpstreamUnavailable
            Status.Code.DEADLINE_EXCEEDED -> ApiErrorCode.UpstreamTimeout
            else -> ApiErrorCode.UpstreamFailure
        }

    return ApiHttpException(
        status = errorCode.status,
        message = status.description ?: "Identity service request failed",
    )
}
