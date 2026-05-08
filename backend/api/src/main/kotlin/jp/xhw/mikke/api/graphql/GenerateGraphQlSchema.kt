package jp.xhw.mikke.api.graphql

import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.expediagroup.graphql.generator.TopLevelObject
import com.expediagroup.graphql.generator.extensions.print
import com.expediagroup.graphql.generator.toSchema
import java.nio.file.Files
import java.nio.file.Path

fun main(args: Array<String>) {
    val outputPath = args.firstOrNull() ?: DEFAULT_SCHEMA_PATH
    val schemaPath = Path.of(outputPath)
    schemaPath.parent?.let(Files::createDirectories)

    val graphQL =
        toSchema(
            config =
                SchemaGeneratorConfig(
                    supportedPackages = apiGraphQlPackages,
                ),
            queries =
                apiGraphQlQueryTypes().map {
                    TopLevelObject(it)
                },
            mutations =
                apiGraphQlMutationTypes().map {
                    TopLevelObject(it)
                },
        )

    Files.writeString(schemaPath, graphQL.print() + "\n")
}

private const val DEFAULT_SCHEMA_PATH = "src/main/graphql/schema.graphqls"
