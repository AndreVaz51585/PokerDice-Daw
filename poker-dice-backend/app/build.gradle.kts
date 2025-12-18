plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
}

group = "pt.isel"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":poker-dice-backend:http"))
    api(project(":poker-dice-backend:service"))
    implementation(project(":poker-dice-backend:SQL"))
    implementation("org.jdbi:jdbi3-core:3.43.0")
    implementation("org.jdbi:jdbi3-kotlin:3.43.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.7.3")

    // BOM do Spring Boot fixa todas as versões (Spring, Jackson, etc.)
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.6"))

    // Web + MVC + Jackson + Servlet API (tudo transitivo)
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Security core (sem versão — vem do BOM)
    api("org.springframework.security:spring-security-core")

    // Testes (sem versão — vem do BOM)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("io.projectreactor:reactor-test:3.5.13") // StepVerifier
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
