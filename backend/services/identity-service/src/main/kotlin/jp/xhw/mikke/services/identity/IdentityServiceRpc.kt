package jp.xhw.mikke.services.identity

import io.grpc.Status
import jp.xhw.mikke.identity.v1.GetMeRequest
import jp.xhw.mikke.identity.v1.GetMeResponse
import jp.xhw.mikke.identity.v1.IdentityServiceGrpcKt
import jp.xhw.mikke.identity.v1.LoginUserRequest
import jp.xhw.mikke.identity.v1.LoginUserResponse
import jp.xhw.mikke.identity.v1.RefreshSessionRequest
import jp.xhw.mikke.identity.v1.RefreshSessionResponse
import jp.xhw.mikke.identity.v1.RegisterUserRequest
import jp.xhw.mikke.identity.v1.RegisterUserResponse
import jp.xhw.mikke.platform.auth.grpc.GrpcAuthContext
import jp.xhw.mikke.services.identity.application.DuplicateIdentityUserException
import jp.xhw.mikke.services.identity.application.IdentityApplicationException
import jp.xhw.mikke.services.identity.application.IdentityService
import jp.xhw.mikke.services.identity.application.InvalidCredentialsException
import jp.xhw.mikke.services.identity.application.InvalidIdentityInputException
import jp.xhw.mikke.services.identity.application.InvalidRefreshTokenException
import jp.xhw.mikke.services.identity.application.LoginIdentityUserCommand
import jp.xhw.mikke.services.identity.application.RegisterIdentityUserCommand
import jp.xhw.mikke.services.identity.application.UserNotFoundException

class IdentityServiceRpc(
    private val identityService: IdentityService,
) : IdentityServiceGrpcKt.IdentityServiceCoroutineImplBase() {
    override suspend fun registerUser(request: RegisterUserRequest): RegisterUserResponse {
        val result =
            execute {
                identityService.register(
                    RegisterIdentityUserCommand(
                        email = request.email.requireField("email"),
                        username = request.username.requireField("username"),
                        displayName = request.displayName.requireField("display_name"),
                        password = request.password.requireField("password"),
                    ),
                )
            }

        return RegisterUserResponse
            .newBuilder()
            .setUser(result.user.toProto())
            .setSession(result.session.toProto())
            .build()
    }

    override suspend fun loginUser(request: LoginUserRequest): LoginUserResponse {
        val result =
            execute {
                identityService.login(
                    LoginIdentityUserCommand(
                        loginId = request.loginId.requireField("login_id"),
                        password = request.password.requireField("password"),
                    ),
                )
            }

        return LoginUserResponse
            .newBuilder()
            .setUser(result.user.toProto())
            .setSession(result.session.toProto())
            .build()
    }

    override suspend fun refreshSession(request: RefreshSessionRequest): RefreshSessionResponse {
        val session = execute { identityService.refreshSession(request.refreshToken.requireField("refresh_token")) }

        return RefreshSessionResponse
            .newBuilder()
            .setSession(session.toProto())
            .build()
    }

    override suspend fun getMe(request: GetMeRequest): GetMeResponse {
        val principal =
            GrpcAuthContext.currentPrincipal()
                ?: throw Status.UNAUTHENTICATED.withDescription("Authentication required").asRuntimeException()

        val user = execute { identityService.getMe(principal.subject) }

        return GetMeResponse
            .newBuilder()
            .setUser(user.toProto())
            .build()
    }
}

private fun String.requireField(fieldName: String): String =
    trim().takeIf { it.isNotEmpty() }
        ?: throw Status.INVALID_ARGUMENT.withDescription("$fieldName is required").asRuntimeException()

private inline fun <T> execute(block: () -> T): T =
    try {
        block()
    } catch (e: IdentityApplicationException) {
        throw e.toStatus().asRuntimeException()
    }

private fun IdentityApplicationException.toStatus(): Status =
    when (this) {
        is InvalidIdentityInputException -> Status.INVALID_ARGUMENT.withDescription(message)
        is DuplicateIdentityUserException -> Status.ALREADY_EXISTS.withDescription(message)
        is InvalidCredentialsException -> Status.UNAUTHENTICATED.withDescription(message)
        is InvalidRefreshTokenException -> Status.UNAUTHENTICATED.withDescription(message)
        is UserNotFoundException -> Status.NOT_FOUND.withDescription(message)
    }
