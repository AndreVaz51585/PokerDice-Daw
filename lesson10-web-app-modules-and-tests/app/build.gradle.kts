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
    implementation(project(":lesson10-web-app-modules-and-tests:http"))
    api(project(":lesson10-web-app-modules-and-tests:service"))
    implementation(project(":SQL"))
    implementation("org.jdbi:jdbi3-core:3.43.0")


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
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
