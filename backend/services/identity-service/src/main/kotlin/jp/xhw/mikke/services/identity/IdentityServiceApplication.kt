package jp.xhw.mikke.services.identity

import jp.xhw.mikke.platform.grpc.grpcServer
import jp.xhw.mikke.platform.grpc.startAndAwait

fun main() {
    grpcServer(serviceName = "identity-service", portEnv = "IDENTITY_SERVICE_PORT", defaultPort = 50051)
        .startAndAwait()
}
