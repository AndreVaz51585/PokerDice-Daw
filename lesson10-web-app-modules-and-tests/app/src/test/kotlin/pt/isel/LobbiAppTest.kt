package pt.isel

import org.junit.jupiter.api.Assertions.assertTrue
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
import java.time.Clock
import java.time.ZoneId

@TestConfiguration
class LobbyAppTestConfig {
    /**
     * Replace Clock in main WebApp configuration
     */
    @Bean
    @Primary
    fun testClock(): Clock = Clock.system(ZoneId.systemDefault())
}

@SpringBootTest(
    classes = [WebApp::class, LobbyAppTestConfig::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LobbyAppTest {

    @Autowired
    lateinit var repoUsers: RepositoryUser

    @Autowired
    lateinit var repoLobbies: RepositoryLobby

    @LocalServerPort
    var port: Int = 0

    private lateinit var client: WebTestClient
    private lateinit var token1: String
    private lateinit var token2: String

    private val userInput1 = UserInput(
        name = "Player One",
        email = "player@one.com",
        password = "secret"
    )

    private val userInput2 = UserInput(
        name = "Player Two",
        email = "player@two.com",
        password = "secret"
    )

    @BeforeAll
    fun setup() {
        // Clear data before test
        repoLobbies.clear()
        repoUsers.clear()

        client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // Create user
        client.post()
            .uri("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(userInput1)
            .exchange()
            .expectStatus()
            .isCreated

        client.post()
            .uri("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(userInput2)
            .exchange()
            .expectStatus()
            .isCreated

        // Obtain token
        val result1 =
            client.post()
                .uri("/users/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    mapOf(
                        "email" to userInput1.email,
                        "password" to userInput1.password
                    )
                ).exchange()
                .expectStatus().isOk
                .expectBody(UserCreateTokenOutputModel::class.java)
                .returnResult()
                .responseBody!!

        token1 = result1.token

        val result2 =
            client.post()
                .uri("/users/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    mapOf(
                        "email" to userInput2.email,
                        "password" to userInput2.password
                    )
                ).exchange()
                .expectStatus().isOk
                .expectBody(UserCreateTokenOutputModel::class.java)
                .returnResult()
                .responseBody!!

        token2 = result2.token
    }

    @Test
    fun `can create lobby, list it, fetch by id, and join it`() {
        val lobbyInput = LobbyInput(
            name = "Fun Lobby",
            description = "For testing purposes",
            minPlayers = 2,
            maxPlayers = 4,
            rounds = 3,
            ante = 100
        )

        // Create a lobby
        val locationHeader =
            client.post()
                .uri("/lobbies")
                .header("Authorization", "Bearer $token1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(lobbyInput)
                .exchange()
                .expectStatus().isCreated
                .expectHeader()
                .value("location") { assertTrue(it.startsWith("/api/lobbies/")) }
                .returnResult(Void::class.java)
                .responseHeaders
                .location!!
                .path

        // Extract ID from location
        val lobbyId = locationHeader.substringAfterLast("/").toInt()

        // Fetch all lobbies
        client.get()
            .uri("/lobbies")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$[0].name").isEqualTo("Fun Lobby")
            .jsonPath("$[0].description").isEqualTo("For testing purposes")

        // Fetch by ID
        client.get()
            .uri("/lobbies/$lobbyId")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("id").isEqualTo(lobbyId)
            .jsonPath("name").isEqualTo("Fun Lobby")

        // other User can Join the Lobby
        client.post()
            .uri("/lobbies/$lobbyId/join")
            .header("Authorization", "Bearer $token2")
            .exchange()
            .expectStatus().isOk
            .expectBody(Boolean::class.java)
            .isEqualTo(true)
    }

    @Test
    fun `should return 404 when getting non-existent lobby`() {
        client.get()
            .uri("/lobbies/99999")
            .exchange()
            .expectStatus().isNotFound
            .expectHeader()
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("title")
            .isEqualTo("lobby-not-found")
    }

    @Test
    fun `should return 404 when joining non-existent lobby`() {
        client.post()
            .uri("/lobbies/99999/join")
            .header("Authorization", "Bearer $token1")
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("title")
            .isEqualTo("lobby-not-found")
    }
}
