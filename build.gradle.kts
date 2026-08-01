plugins {
    id("java")
    id("xyz.jpenilla.run-paper") version "3.0.2" // Useful for running a local Paper test server.
}

group = "at.lowdfx"
version = "2.1.0"

java {
    // Compile against the oldest supported runtime so one JAR works on both
    // Paper 1.21.11 (Java 21) and Paper 26.1.2 (Java 25).
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.15.2")
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val java25Launcher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(25))
}

tasks {
    test {
        useJUnitPlatform()
    }

    runServer {
        dependsOn(jar)
        minecraftVersion("1.21.11")
        runDirectory.set(layout.projectDirectory.dir("run/1.21.11"))
    }

    register<xyz.jpenilla.runpaper.task.RunServer>("runServer2612") {
        dependsOn(jar)
        pluginJars(jar.flatMap { it.archiveFile })
        minecraftVersion("26.1.2")
        runDirectory.set(layout.projectDirectory.dir("run/26.1.2"))
        javaLauncher.set(java25Launcher)
    }
}
