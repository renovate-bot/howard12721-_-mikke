package jp.xhw.mikke.api.auth.presentation

import jp.xhw.mikke.api.auth.application.LoginResult
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val loginId: String = "",
    val password: String = "",
)

@Serializable
data class LoginResponse(
    val user: AuthenticatedUserResponse,
    val session: AuthSessionResponse,
)

@Serializable
data class AuthenticatedUserResponse(
    val id: String,
    val email: String,
    val username: String,
    val displayName: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class AuthSessionResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAt: String,
    val refreshTokenExpiresAt: String,
)

fun LoginResult.toResponse(): LoginResponse =
    LoginResponse(
        user =
            AuthenticatedUserResponse(
                id = user.id,
                email = user.email,
                username = user.username,
                displayName = user.displayName,
                status = user.status,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt,
            ),
        session =
            AuthSessionResponse(
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
                accessTokenExpiresAt = session.accessTokenExpiresAt,
                refreshTokenExpiresAt = session.refreshTokenExpiresAt,
            ),
    )
