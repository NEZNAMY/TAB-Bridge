plugins {
    id("dev.architectury.loom")
}

repositories {
    // Gradle doesn't support combining settings and project repositories, so we have to re-declare all the settings repos we need
    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://maven.nucleoid.xyz/")
}

val minecraftVersion = "1.18.2"
val fabricApiVersion = "0.77.0+1.18.2"

dependencies {
    minecraft("com.mojang:minecraft:${minecraftVersion}")
    mappings(loom.officialMojangMappings())
    api(projects.fabric)
    api(projects.shared)
    modImplementation("net.fabricmc:fabric-loader:0.15.10")
    modImplementation(fabricApi.module("fabric-networking-api-v1", fabricApiVersion))
}

tasks {
    compileJava {
        options.release.set(17)
    }
}