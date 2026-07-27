plugins {
    id("java")
    id("xyz.jpenilla.run-paper") version "2.2.3" // Useful for running a local Paper test server.
}

group = "at.lowdfx"
version = "2.0.1"

java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

tasks {
    runServer {
        dependsOn(jar)
        minecraftVersion("1.21.11")
    }
}
