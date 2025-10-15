package pt.isel.app

import org.jdbi.v3.core.Jdbi
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import pt.isel.domain.authentication.Sha256TokenEncoder
import pt.isel.domain.user.UsersDomainConfig
import pt.isel.http.AuthenticatedUserArgumentResolver
import pt.isel.http.AuthenticationInterceptor
import pt.isel.repo.RepositoryLobby
import pt.isel.repo.RepositoryMatch
import pt.isel.repo.jdbi.RepositoryLobbyJdbi
import pt.isel.repo.jdbi.RepositoryMatchJdbi
import pt.isel.repo.jdbi.TransactionManagerJdbi
import pt.isel.repo.mem.RepositoryUserInMem
import java.time.Clock
import java.time.Duration

@Configuration
class PipelineConfigurer(
    val authenticationInterceptor: AuthenticationInterceptor,
    val authenticatedUserArgumentResolver: AuthenticatedUserArgumentResolver,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(authenticationInterceptor)
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(authenticatedUserArgumentResolver)
    }
}

@SpringBootApplication(scanBasePackages = ["pt.isel"])
class WebApp {
    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()

    @Bean
    fun tokenEncoder() = Sha256TokenEncoder()

    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun repositoryUser() = RepositoryUserInMem()

    @Bean
    fun jdbi() : Jdbi = Jdbi.create("jdbc:postgresql://localhost:5432/db", "dbuser", "changeit")

    @Bean
    fun repositoryLobby(jdbi : Jdbi) : RepositoryLobby = RepositoryLobbyJdbi(jdbi.open())


    @Bean
    fun transactionManager(jdbi: Jdbi): pt.isel.repo.TransactionManager =
        TransactionManagerJdbi(jdbi)

    // kotlin
    @Bean
    fun repositoryMatch(jdbi: Jdbi): RepositoryMatch = RepositoryMatchJdbi(jdbi.open())


    @Bean
    fun usersDomainConfig() =
        UsersDomainConfig(
            tokenSizeInBytes = 256 / 8,
            tokenTtl = Duration.ofHours(24),
            tokenRollingTtl = Duration.ofHours(1),
            maxTokensPerUser = 3,
        )
}

fun main() {
    runApplication<WebApp>()
}
