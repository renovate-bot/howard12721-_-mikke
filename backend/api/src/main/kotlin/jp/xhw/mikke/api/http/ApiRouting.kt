package jp.xhw.mikke.api.http

import io.ktor.server.application.*
import io.ktor.server.routing.*
import jp.xhw.mikke.api.auth.presentation.installAuthRoutes
import jp.xhw.mikke.api.bootstrap.ApiDependencies

fun Application.configureApiRouting(dependencies: ApiDependencies) {
    routing {
        installAuthRoutes(authApiService = dependencies.authApiService)
    }
}
