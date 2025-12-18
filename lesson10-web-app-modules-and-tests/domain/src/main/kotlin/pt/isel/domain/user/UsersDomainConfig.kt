package pt.isel.domain.user

import java.time.Duration

data class UsersDomainConfig(
    val tokenSizeInBytes: Int,  // 32 bytes (256 bits)
    val tokenTtl: Duration,     // 24 horas
    val tokenRollingTtl: Duration,  // 1 horas
    val maxTokensPerUser: Int,  // 3 tokens simultâneos
) {
    init {
        require(tokenSizeInBytes > 0)
        require(tokenTtl.isPositive)
        require(tokenRollingTtl.isPositive)
        require(maxTokensPerUser > 0)
    }
}
