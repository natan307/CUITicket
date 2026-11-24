plugins {
    java
    id("com.gradleup.shadow") version "9.3.1"
}

group = "io.github.natan307"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://jitpack.io")
}

dependencies {
    // Standard Spigot API: runs on Spigot, Paper, Purpur and Folia.
    compileOnly("org.spigotmc:spigot-api:1.21.4-R0.1-SNAPSHOT")
    // CrossUI provides the Bedrock forms, Java dialogs and chest fallback.
    compileOnly("com.github.natan307:CrossUI:2.0.1")
    // bStats is bundled and relocated into the jar.
    implementation("org.bstats:bstats-bukkit:3.2.1")
}

java {
    // Bytecode is Java 21, so one jar runs on 1.21.x (Java 21) and 26.x (Java 25) servers.
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filesMatching(listOf("plugin.yml", "plugin-version.txt")) {
        expand(props)
    }
}

tasks.jar {
    archiveClassifier.set("plain")
}

tasks.shadowJar {
    archiveBaseName.set("CUITicket")
    archiveClassifier.set("")
    relocate("org.bstats", "io.github.natan307.cuiticket.libs.bstats")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
