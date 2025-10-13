//package pt.isel.service
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.ComponentScan
//import org.springframework.context.annotation.Configuration
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
//import pt.isel.domain.authentication.Sha256TokenEncoder
//import pt.isel.domain.user.UsersDomainConfig
//import pt.isel.repo.jdbi.TransactionManagerJdbi
//import java.time.Clock
//import java.time.Duration
//
//@Configuration
//@ComponentScan("pt.isel")
//class TestConfig {
//
//    @Bean
//    fun passwordEncoder() = BCryptPasswordEncoder()
//
//    @Bean
//    fun tokenEncoder() = Sha256TokenEncoder()
//
//    @Bean
//    fun clock(): Clock = Clock.systemUTC()
//
//    @Bean
//    fun trxManager() = TransactionManagerJdbi(Environment.getDbUrl())
//
//    @Bean
//    fun usersDomainConfig() =
//        UsersDomainConfig(
//            tokenSizeInBytes = 256 / 8,
//            tokenTtl = Duration.ofHours(24),
//            tokenRollingTtl = Duration.ofHours(1),
//            maxTokensPerUser = 3,
//        )
//}
