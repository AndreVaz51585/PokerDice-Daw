/*package pt.isel

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
import pt.isel.http.model.lobby.LobbyInput
import pt.isel.http.model.user.UserCreateTokenOutputModel
import pt.isel.http.model.user.UserInput
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
    private lateinit var token1: String // user 1
    private lateinit var token2: String // user 2
    private lateinit var token3: String // user 3
    private lateinit var token4: String // user 4

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

    private val userInput3 = UserInput(
        name = "Player Three",
        email = "Player@three.com",
        password = "secret")

    private val userInput4 = UserInput(
        name = "Player four",
        email = "Player@four.com",
        password = "secret")

    @BeforeAll
    fun setup() {
        // Clear data before test
        repoLobbies.clear()
        repoUsers.clear()

        client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // Create user 1
        client.post()
            .uri("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(userInput1)
            .exchange()
            .expectStatus()
            .isCreated

        // create user 2
        client.post()
            .uri("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(userInput2)
            .exchange()
            .expectStatus()
            .isCreated

        // create user 3
        client.post()
            .uri("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(userInput3)
            .exchange()
            .expectStatus()
            .isCreated

        // create user 4
        client.post()
            .uri("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(userInput4)
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

        val result3 =
            client.post()
                .uri("/users/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    mapOf(
                        "email" to userInput3.email,
                        "password" to userInput3.password
                    )
                ).exchange()
                .expectStatus().isOk
                .expectBody(UserCreateTokenOutputModel::class.java)
                .returnResult()
                .responseBody!!

        token3 = result3.token

        val result4 =
            client.post()
                .uri("/users/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    mapOf(
                        "email" to userInput4.email,
                        "password" to userInput4.password
                    )
                ).exchange()
                .expectStatus().isOk
                .expectBody(UserCreateTokenOutputModel::class.java)
                .returnResult()
                .responseBody!!

        token4 = result4.token
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
                .header("Authorization", "Bearer $token1") // user1 cria o lobby
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
            .expectBody(String::class.java)
            .isEqualTo("Player Added to Lobby")
    }


    @Test
    fun `when the maxPlayers is reached the match starts and the LobbyState updates to Full`() {
        val lobbyInput = LobbyInput(
            name = "Full Lobby",
            description = "For testing purposes",
            minPlayers = 2,
            maxPlayers = 3,
            rounds = 3,
            ante = 100
        )

        // Create a lobby
        val locationHeader =
            client.post()
                .uri("/lobbies")
                .header("Authorization", "Bearer $token3") // user3 cria o lobby
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

        // user1 Joins the Lobby
        client.post()
            .uri("/lobbies/$lobbyId/join")
            .header("Authorization", "Bearer $token1")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .isEqualTo("Player Added to Lobby")

        // user2 Joins the Lobby - this should start the match
        client.post()
            .uri("/lobbies/$lobbyId/join")
            .header("Authorization", "Bearer $token2")
            .exchange()
            .expectStatus().isCreated
            .expectHeader()
            .value("location") { assertTrue(it.startsWith("/api/matches/")) }
            .expectBody()
            .jsonPath("matchId").exists()

        // user4 tries to Join the Lobby - but its already full
        client.post()
            .uri("/lobbies/$lobbyId/join")
            .header("Authorization", "Bearer $token4")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("title")
            .isEqualTo("lobby-full")



    }

    @Test
    fun `should remove players and delete the lobby when the lobby host leaves while the LobbyState is Open`() {
        // First, clear all lobbies
        val lobbyInput = LobbyInput(
            name = "Host to leave Lobby",
            description = "For testing purposes",
            minPlayers = 2,
            maxPlayers = 4,
            rounds = 3,
            ante = 100
        )

        val locationHeader =
            client.post()
                .uri("/lobbies")
                .header("Authorization", "Bearer $token1") // user3 cria o lobby
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

        val lobbyId = locationHeader.substringAfterLast("/").toInt()

        // user2 Joins the Lobby
        client.post()
            .uri("/lobbies/$lobbyId/join")
            .header("Authorization", "Bearer $token2")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .isEqualTo("Player Added to Lobby")

        //user3 joins the lobby

        client.post()
            .uri("/lobbies/$lobbyId/join")
            .header("Authorization", "Bearer $token3")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .isEqualTo("Player Added to Lobby")


        // Lobby Host leaves the lobby
        client.delete()
            .uri("/lobbies/$lobbyId")
            .header("Authorization","Bearer $token1")
            .exchange()
            .expectStatus().isOk

        // Verify that the lobby has been deleted
        client.get()
            .uri("/lobbies/$lobbyId")
            .exchange()
            .expectStatus().isNotFound



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
*/