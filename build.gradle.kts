import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("jvm") version "1.9.20"
}

group = "dev.kunet"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
    compileOnly("org.spigotmc:spigot:1.8.8-R0.1-SNAPSHOT")
    compileOnly("com.google.code.gson:gson:2.10.1")
    implementation(kotlin("stdlib-jdk8"))

    implementation("com.github.retrooper.packetevents:spigot:2.1.0")
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    withType<ShadowJar> {
        exclude("META-INF/**")
        exclude("DebugProbesKt.bin")

        relocate("com.github.retrooper.packetevents", "dev.kunet.skyheads.packetevents.api")
        relocate("io.github.retrooper.packetevents", "dev.kunet.skyheads.packetevents.impl")
        relocate("net.kyori", "dev.kunet.skyheads.packetevents.kyori")
    }
}

kotlin {
    jvmToolchain(8)
}
