package jp.xhw.mikke.api

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*

fun main() {
    apiServer().start(wait = true)
}

internal fun apiServer(port: Int = System.getenv("PORT")?.toIntOrNull() ?: 8080): ApplicationEngine =
    embeddedServer(factory = Netty, port = port, module = Application::apiModule).engine

fun Application.apiModule() {
    install(CallLogging)
    install(ContentNegotiation) {
        json()
    }
}
