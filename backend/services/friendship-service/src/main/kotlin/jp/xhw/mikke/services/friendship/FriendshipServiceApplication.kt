package jp.xhw.mikke.services.friendship

import jp.xhw.mikke.platform.grpc.grpcServer
import jp.xhw.mikke.platform.grpc.startAndAwait

fun main() {
    grpcServer(serviceName = "friendship-service", portEnv = "FRIENDSHIP_SERVICE_PORT", defaultPort = 50052)
        .startAndAwait()
}
