plugins {
    id("dev.architectury.loom-no-remap")
}

repositories {
    // Gradle doesn't support combining settings and project repositories, so we have to re-declare all the settings repos we need
    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://maven.nucleoid.xyz/")
}

val minecraftVersion = "26.1.2"

// Fabric API versions for each Minecraft version for easier backporting
// Official website (for updating in the future): https://fabricmc.net/develop/
val fabricApiVersions = mapOf(
    "26.1.2" to "0.146.1+26.1.2"
)

dependencies {
    minecraft("com.mojang:minecraft:${minecraftVersion}")
    api(projects.shared)
    implementation("me.lucko:fabric-permissions-api:0.7.0")
    implementation("eu.pb4:placeholder-api:3.0.0+26.1")
    implementation("net.fabricmc:fabric-loader:0.19.2")
    implementation(fabricApi.module("fabric-lifecycle-events-v1", fabricApiVersions.getValue(minecraftVersion)))
    implementation(fabricApi.module("fabric-networking-api-v1", fabricApiVersions.getValue(minecraftVersion)))
    implementation(fabricApi.module("fabric-entity-events-v1", fabricApiVersions.getValue(minecraftVersion)))
}

tasks {
    compileJava {
        options.release.set(25)
    }
}