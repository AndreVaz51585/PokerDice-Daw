//package pt.isel.service.lobbyService
//
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.BeforeEach
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
//import pt.isel.service.TestConfig
//import pt.isel.repo.TransactionManager
//import pt.isel.domain.Game.Lobby.LobbyState
//import pt.isel.service.Auxiliary.Success
//import kotlin.test.assertIs
//import kotlin.test.assertTrue
//
//@SpringJUnitConfig(TestConfig::class)
//class LobbyServiceIntegrationTest {
//
//    @Autowired
//    private lateinit var lobbyService: LobbyService
//
//    @Autowired
//    private lateinit var trxManager: TransactionManager
//
//    @BeforeEach
//    fun setup() {
//        trxManager.run {
//            repoLobbies.clear()  // ou limpa as tabelas na BD
//            repoUsers.clear()
//        }
//    }
//
//    @Test
//    fun `should create lobby successfully when host exists`() {
//        val user = repoUser.createUser("John", "john@example.com", "pass")
//        val result = lobbyService.createLobby(
//            hostId = user.id,
//            name = "Test Lobby",
//            description = "desc",
//            minPlayers = 2,
//            maxPlayers = 4,
//            rounds = 3,
//            ante = 100,
//            state = LobbyState.OPEN
//        )
//
//        assertIs<Success<*>>(result)
//        assertTrue(result.na== "Test Lobby")
//    }
//}
