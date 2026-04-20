package jp.xhw.mikke.platform.auth.ktor

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import jp.xhw.mikke.platform.auth.AuthenticatedPrincipal

fun ApplicationCall.authenticatedPrincipal(): AuthenticatedPrincipal? =
    principal<AuthenticatedPrincipal>() ?: principal<JWTPrincipal>()?.toAuthenticatedPrincipal()

fun ApplicationCall.requireAuthenticatedPrincipal(): AuthenticatedPrincipal =
    authenticatedPrincipal() ?: error("AuthenticatedPrincipal is not available on this call")
