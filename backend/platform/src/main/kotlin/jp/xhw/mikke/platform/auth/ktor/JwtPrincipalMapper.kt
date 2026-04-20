package jp.xhw.mikke.platform.auth.ktor

import io.ktor.server.auth.jwt.*
import jp.xhw.mikke.platform.auth.AuthenticatedPrincipal

fun JWTPrincipal.toAuthenticatedPrincipal(): AuthenticatedPrincipal {
    val jwt = payload
    val subject =
        jwt.subject
            ?: error("JWT sub claim is required to create AuthenticatedPrincipal")

    return AuthenticatedPrincipal(subject = subject)
}
