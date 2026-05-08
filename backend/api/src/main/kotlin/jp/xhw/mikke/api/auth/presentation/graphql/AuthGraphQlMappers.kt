package jp.xhw.mikke.api.auth.presentation.graphql

import jp.xhw.mikke.api.auth.application.LoginResult
import jp.xhw.mikke.api.auth.application.RefreshResult
import jp.xhw.mikke.api.auth.application.RegisterResult
import jp.xhw.mikke.api.auth.application.AuthSession as AuthSessionResult

fun LoginResult.toGraphQl(): AuthPayload = AuthPayload(session = session.toGraphQl())

fun RegisterResult.toGraphQl(): AuthPayload = AuthPayload(session = session.toGraphQl())

fun RefreshResult.toGraphQl(): RefreshAuthPayload = RefreshAuthPayload(session = session.toGraphQl())

private fun AuthSessionResult.toGraphQl(): AuthSession =
    AuthSession(
        accessToken = accessToken,
        refreshToken = refreshToken,
        accessTokenExpiresAt = accessTokenExpiresAt,
        refreshTokenExpiresAt = refreshTokenExpiresAt,
    )
