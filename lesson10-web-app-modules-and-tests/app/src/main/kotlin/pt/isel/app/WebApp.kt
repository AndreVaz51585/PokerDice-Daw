package pt.isel.app

import MatchServiceImpl
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import pt.isel.domain.authentication.PasswordValidationInfo
import pt.isel.domain.authentication.Sha256TokenEncoder
import pt.isel.domain.token.TokenValidationInfo
import pt.isel.domain.user.UsersDomainConfig
import pt.isel.http.argumentResolverandInterceptor.AuthenticatedUserArgumentResolver
import pt.isel.http.argumentResolverandInterceptor.AuthenticationInterceptor
import pt.isel.mapper.PasswordValidationInfoMapper
import pt.isel.mapper.TokenValidationInfoMapper
import pt.isel.repo.*
import pt.isel.repo.jdbi.*
import pt.isel.service.matchService.MatchManager
import pt.isel.service.matchService.MatchService
import pt.isel.service.walletService.WalletService
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
    fun repositoryUser(jdbi : Jdbi) : RepositoryUser = RepositoryUserJdbi(jdbi.open())

    @Bean
    fun jdbi(): Jdbi {
        val jdbi = Jdbi.create("jdbc:postgresql://localhost:5432/db", "dbuser", "changeit")
        jdbi.installPlugin(KotlinPlugin()) // necessário para realizar o mapeamento automático para data class
        jdbi.registerColumnMapper(PasswordValidationInfo::class.java, PasswordValidationInfoMapper())
        jdbi.registerColumnMapper(TokenValidationInfo::class.java, TokenValidationInfoMapper())
        return jdbi
    }

    @Bean
    fun repositoryLobby(jdbi : Jdbi) : RepositoryLobby = RepositoryLobbyJdbi(jdbi.open())

    @Bean
    fun repositoryWallet(jdbi : Jdbi) : RepositoryWallet = RepositoryWalletJdbi(jdbi.open())


    @Bean
    fun transactionManager(jdbi: Jdbi): TransactionManager =
        TransactionManagerJdbi(jdbi)

    // kotlin
    @Bean
    fun repositoryMatch(jdbi: Jdbi): RepositoryMatch = RepositoryMatchJdbi(
        jdbi.open(),
        repoLobby = RepositoryLobbyJdbi(jdbi.open())
    )

    @Bean
    fun matchManager(): MatchManager = MatchManager()

    @Bean
    fun matchService(
        repoMatch: RepositoryMatch,
        trxManager: TransactionManager,
        matchManager: MatchManager,
        repoLobby: RepositoryLobby,
        walletService: WalletService,
    ): MatchService {
        return MatchServiceImpl(repoLobby,repoMatch, walletService ,trxManager, matchManager)
    }

    @Bean
    fun usersDomainConfig() =
        UsersDomainConfig(
            tokenSizeInBytes = 256 / 8,
            tokenTtl = Duration.ofHours(24),
            tokenRollingTtl = Duration.ofHours(1),
            maxTokensPerUser = 3,
        )

    @Bean
    fun repositoryInvitation(jdbi: Jdbi) = RepositoryInvitationJdbi(
        jdbi.open(),
        repoUser = RepositoryUserJdbi(jdbi.open())
    )

}

fun main() {
    runApplication<WebApp>()
}
