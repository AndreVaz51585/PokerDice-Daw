dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}


plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

rootProject.name = "daw2025"
/*
include("lesson01-intro-DAW")
include("lesson02-movies")
include("lesson03-spring-context")
include("lesson03-ioc-and-di-container")
include("lesson03-movies-in-ioc-container")
include("lesson05-lab-spring-mvc")
include("lesson07-servlet-api")
include("lesson08-spring-web-pipeline")
include("lesson09-spring-and-auth")
*/

// nested modules under `lesson10-web-app-modules-and-tests`
include("poker-dice-backend:app")
include("poker-dice-backend:domain")
include("poker-dice-backend:http")
include("poker-dice-backend:service")
include("poker-dice-backend:repo")
include("poker-dice-backend:SQL")

