package jp.xhw.mikke.platform.grpc

import io.grpc.Server
import io.grpc.ServerBuilder

fun grpcServer(
    serviceName: String,
    portEnv: String,
    defaultPort: Int,
    configure: ServerBuilder<*>.() -> Unit = {},
): Server {
    val port = System.getenv(portEnv)?.toIntOrNull() ?: defaultPort

    return ServerBuilder
        .forPort(port)
        .apply(configure)
        .build()
        .also { server ->
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    server.shutdown()
                },
            )
            println("$serviceName listening on port $port")
        }
}

fun Server.startAndAwait() {
    start()
    awaitTermination()
}
