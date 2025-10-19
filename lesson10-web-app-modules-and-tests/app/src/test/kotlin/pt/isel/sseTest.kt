package pt.isel

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import pt.isel.app.WebApp
import pt.isel.http.model.LobbyInput
import pt.isel.http.model.UserCreateTokenOutputModel
import pt.isel.http.model.UserInput
import pt.isel.repo.RepositoryLobby
import pt.isel.repo.RepositoryUser
import reactor.test.StepVerifier
import java.time.Clock
import java.time.Duration
import java.time.ZoneId

@TestConfiguration
class MatchSseTestConfig {
    @Bean
    @Primary
    fun testClock(): Clock = Clock.system(ZoneId.systemDefault())
}

@SpringBootTest(
    classes = [WebApp::class, MatchSseTestConfig::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MatchSseIntegrationTest {

    @Autowired
    lateinit var repoUsers: RepositoryUser

    @Autowired
    lateinit var repoLobbies: RepositoryLobby

    private lateinit var client: WebTestClient
    private lateinit var token1: String
    private lateinit var token2: String
    private lateinit var token3: String
    private var userId1 = 0
    private var userId2 = 0
    private var userId3 = 0

    @LocalServerPort
    var port: Int = 0

    @BeforeAll
    fun setup() {
        repoUsers.clear()
        repoLobbies.clear()

        client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").responseTimeout(Duration.ofSeconds(30)).build()
        // Create Users
        val user1 = UserInput("Player One", "player1@test.com", "secret")
        val user2 = UserInput("Player Two", "player2@test.com", "secret")
        val user3 = UserInput("Player Three", "player3@test.com", "secret")

        userId1 = client.post().uri("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(user1)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .returnResult()
            .responseHeaders["location"]!!.last().substringAfterLast("/").toInt()

        userId2 = client.post().uri("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(user2)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .returnResult()
            .responseHeaders["location"]!!.last().substringAfterLast("/").toInt()

        userId3 = client.post().uri("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(user3)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .returnResult()
            .responseHeaders["location"]!!.last().substringAfterLast("/").toInt()

        // Get tokens
        token1 = getToken(user1)
        token2 = getToken(user2)
        token3 = getToken(user3)
    }

    private fun getToken(user: UserInput): String {
        val res = client.post().uri("/users/token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("email" to user.email, "password" to user.password))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserCreateTokenOutputModel::class.java)
            .returnResult()
            .responseBody!!
        return res.token
    }

    @Test
    fun `full match flow with SSE`() {
        // 1️⃣ Create Lobby
        val lobbyInput = LobbyInput(
            name = "SSE Lobby",
            description = "Test SSE Match",
            minPlayers = 2,
            maxPlayers = 2,
            rounds = 1,
            ante = 100
        )

        val locationHeader = client.post().uri("/lobbies")
            .header("Authorization", "Bearer $token1")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(lobbyInput)
            .exchange()
            .expectStatus().isCreated
            .returnResult(Void::class.java)
            .responseHeaders
            .location!!
            .path

        val lobbyId = locationHeader.substringAfterLast("/").toInt()


        //Join lobby with player2 automatically starts match\
        val matchLocation = client.post().uri("/lobbies/$lobbyId/join")
            .header("Authorization", "Bearer $token2")
            .exchange()
            .expectStatus().isCreated
            .returnResult(Void::class.java)
            .responseHeaders
            .location!!
            .path

        val matchId = matchLocation.substringAfterLast("/").toInt()

        // 3️⃣ Open SSE to listen to match events
        val eventStream = client.get()
            .uri("/matches/sse/$matchId/events")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .returnResult(String::class.java)
            .responseBody

        // 4️⃣ Send a Roll command for player1
        client.post().uri("/matches/sse/$matchId/events")
            .header("Authorization", "Bearer $token1")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("action" to "Roll", "playerId" to userId1))
            .exchange()
            .expectStatus().isOk

        // 5️⃣ Send a FinishTurn command for player1
        client.post().uri("/matches/sse/$matchId/events")
            .header("Authorization", "Bearer $token1")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("action" to "FinishTurn", "playerId" to userId1))
            .exchange()
            .expectStatus().isOk

        // 6️⃣ Send Roll for player2
        client.post().uri("/matches/sse/$matchId/events")
            .header("Authorization", "Bearer $token2")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("action" to "Roll", "playerId" to userId2))
            .exchange()
            .expectStatus().isOk

        // 7️⃣ FinishTurn for player2
        client.post().uri("/matches/sse/$matchId/events")
            .header("Authorization", "Bearer $token2")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("action" to "FinishTurn", "playerId" to userId2))
            .exchange()
            .expectStatus().isOk

        StepVerifier.create(eventStream)
            .expectNextMatches { it.isNotBlank() }  // Verifica apenas se há algum conteúdo
            .thenCancel()
            .verify()
    }
}
