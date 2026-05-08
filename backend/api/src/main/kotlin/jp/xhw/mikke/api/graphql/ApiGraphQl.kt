package jp.xhw.mikke.api.graphql

import com.expediagroup.graphql.server.ktor.GraphQL
import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import io.ktor.server.application.Application
import io.ktor.server.application.install
import jp.xhw.mikke.api.auth.presentation.graphql.AuthMutation
import jp.xhw.mikke.api.bootstrap.ApiDependencies
import kotlin.reflect.KClass

fun Application.configureApiGraphQl(dependencies: ApiDependencies) {
    install(GraphQL) {
        schema {
            packages = apiGraphQlPackages
            queries = apiGraphQlQueries()
            mutations = apiGraphQlMutations(dependencies)
        }
        engine {
            exceptionHandler = ApiGraphQlExceptionHandler()
        }
    }
}

val apiGraphQlPackages =
    listOf(
        "jp.xhw.mikke.api.graphql",
        "jp.xhw.mikke.api.auth.presentation.graphql",
    )

fun apiGraphQlQueries(): List<Query> = listOf(ApiQuery())

fun apiGraphQlMutations(dependencies: ApiDependencies): List<Mutation> =
    listOf(
        AuthMutation(authApiService = dependencies.authApiService),
    )

fun apiGraphQlQueryTypes(): List<KClass<*>> =
    listOf(
        ApiQuery::class,
    )

fun apiGraphQlMutationTypes(): List<KClass<*>> =
    listOf(
        AuthMutation::class,
    )

class ApiQuery : Query {
    fun health(): String = "ok"
}
