import org.gradle.api.artifacts.VersionCatalogsExtension
import java.io.File

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ktlint)
}

val libsCatalog = the<VersionCatalogsExtension>().named("libs")

ktlint {
    android.set(true)
    filter {
        exclude { element ->
            element.file.path
                .replace(File.separatorChar, '/')
                .contains("/generated/")
        }
    }
}
