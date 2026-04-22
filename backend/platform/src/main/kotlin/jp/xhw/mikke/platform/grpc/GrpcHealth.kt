package jp.xhw.mikke.platform.grpc

import io.grpc.ServerBuilder
import io.grpc.health.v1.HealthCheckResponse.ServingStatus
import io.grpc.protobuf.services.HealthStatusManager

fun ServerBuilder<*>.installGrpcHealth(serviceName: String): HealthStatusManager =
    HealthStatusManager().also { healthStatusManager ->
        addService(healthStatusManager.healthService)
        healthStatusManager.setStatus("", ServingStatus.SERVING)
        healthStatusManager.setStatus(serviceName, ServingStatus.SERVING)
    }
