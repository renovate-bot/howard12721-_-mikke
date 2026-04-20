package jp.xhw.mikke.services.post

import jp.xhw.mikke.platform.grpc.grpcServer
import jp.xhw.mikke.platform.grpc.startAndAwait

fun main() {
    grpcServer(serviceName = "post-service", portEnv = "POST_SERVICE_PORT", defaultPort = 50053)
        .startAndAwait()
}
