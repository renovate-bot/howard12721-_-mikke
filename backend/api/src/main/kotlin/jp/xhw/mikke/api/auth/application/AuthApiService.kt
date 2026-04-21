package jp.xhw.mikke.api.auth.application

import jp.xhw.mikke.api.http.ApiErrorCode
import jp.xhw.mikke.api.http.ApiHttpException

class AuthApiService(
    private val identityAuthGateway: IdentityAuthGateway,
) {
    suspend fun login(command: LoginCommand): LoginResult {
        val loginId = command.loginId.trim()
        if (loginId.isEmpty()) {
            throw ApiHttpException(
                status = ApiErrorCode.InvalidRequest.status,
                message = "loginId is required",
            )
        }
        if (command.password.isBlank()) {
            throw ApiHttpException(
                status = ApiErrorCode.InvalidRequest.status,
                message = "password is required",
            )
        }

        return identityAuthGateway.login(
            command = command.copy(loginId = loginId),
        )
    }
}

data class LoginCommand(
    val loginId: String,
    val password: String,
)

data class LoginResult(
    val user: AuthenticatedUser,
    val session: AuthSession,
)

data class AuthenticatedUser(
    val id: String,
    val email: String,
    val username: String,
    val displayName: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
)

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAt: String,
    val refreshTokenExpiresAt: String,
)
