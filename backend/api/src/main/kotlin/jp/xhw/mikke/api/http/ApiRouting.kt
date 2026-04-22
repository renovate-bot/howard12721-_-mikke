package jp.xhw.mikke.api.http

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import jp.xhw.mikke.api.auth.presentation.installAuthRoutes
import jp.xhw.mikke.api.bootstrap.ApiDependencies
import jp.xhw.mikke.platform.health.HealthResponse

fun Application.configureApiRouting(dependencies: ApiDependencies) {
    routing {
        get("/health") {
            call.respond(HealthResponse(service = "api"))
        }
        installAuthRoutes(authApiService = dependencies.authApiService)
    }
}
