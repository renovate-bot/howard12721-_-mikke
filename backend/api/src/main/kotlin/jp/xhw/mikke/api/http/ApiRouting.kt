package jp.xhw.mikke.api.http

import com.expediagroup.graphql.server.ktor.graphQLPostRoute
import com.expediagroup.graphql.server.ktor.graphQLSDLRoute
import com.expediagroup.graphql.server.ktor.graphiQLRoute
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import jp.xhw.mikke.platform.health.HealthResponse

fun Application.configureApiRouting() {
    routing {
        route("") {
            // Expedia's GraphQL route helpers install their own route-scoped Jackson
            // ContentNegotiation, so REST routes keep Kotlinx negotiation scoped here.
            install(ContentNegotiation) {
                json()
            }

            get("/health") {
                call.respond(HttpStatusCode.OK, HealthResponse(service = "api"))
            }
        }

        graphQLPostRoute(endpoint = "graphql")
        graphQLSDLRoute(endpoint = "graphql/schema")
        graphiQLRoute(
            endpoint = "graphiql",
            graphQLEndpoint = "graphql",
        )
    }
}
