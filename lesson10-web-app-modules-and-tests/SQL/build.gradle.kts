plugins {
    kotlin("jvm") version "1.9.25"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1" // opcional, só se quiseres lint
}

group = "pt.isel"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    // JDBI + PostgreSQL
    implementation("org.jdbi:jdbi3-core:3.37.1")
    implementation("org.jdbi:jdbi3-kotlin:3.37.1")
    implementation("org.jdbi:jdbi3-postgres:3.37.1")
    implementation("org.postgresql:postgresql:42.7.2")

    // Kotlin reflection (necessário para JDBI + data classes)
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Testing
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()

    // URL de ligação ao container postgres
    environment("DB_URL", "jdbc:postgresql://localhost:5432/testdb?user=postgres&password=postgres")

    // Controlar containers automaticamente (não obrigatório, mas útil)
    dependsOn("dbTestsWait")
    finalizedBy("dbTestsDown")
}

kotlin {
    jvmToolchain(21)
}

val composeFileDir: Directory = rootProject.layout.projectDirectory
val dockerComposePath = composeFileDir.file("lesson10-web-app-modules-and-tests/SQL/docker-compose.yml").toString()
val dockerExe =
    when (
        org.gradle.internal.os.OperatingSystem
            .current()
    ) {
        org.gradle.internal.os.OperatingSystem.MAC_OS -> "/usr/local/bin/docker"
        org.gradle.internal.os.OperatingSystem.WINDOWS -> "docker"
        else -> "docker" // Linux and others
    }

tasks.register<Exec>("dbTestsUp") {
    commandLine(dockerExe, "compose", "-f", dockerComposePath, "up", "-d", "--build", "--force-recreate", "db-tests")
}

tasks.register<Exec>("dbTestsWait") {
    commandLine(dockerExe, "exec", "db-tests", "/app/bin/wait-for-postgres.sh", "localhost")
    dependsOn("dbTestsUp")
}

tasks.register<Exec>("dbTestsDown") {
    commandLine(dockerExe, "compose", "-f", dockerComposePath, "down", "db-tests")
}