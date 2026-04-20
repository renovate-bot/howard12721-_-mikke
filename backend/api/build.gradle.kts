plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass = "jp.xhw.mikke.api.ApiApplicationKt"
}

dependencies {
    implementation(project(":platform"))

    implementation(libs.bundles.ktor.server)
    implementation(libs.graphql.kotlin.ktor.server)
    implementation(libs.bundles.grpc.client)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    runtimeOnly(libs.logback.classic)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}
