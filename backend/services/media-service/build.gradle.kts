plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("mikke.protobuf-conventions")
    application
}

application {
    mainClass = "jp.xhw.mikke.services.media.MediaServiceApplicationKt"
}

dependencies {
    implementation(project(":platform"))
    implementation(project(":events:post-events"))

    implementation(libs.bundles.grpc.server)
    implementation(libs.bundles.database)
    implementation(libs.redis.client)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    runtimeOnly(libs.logback.classic)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}
