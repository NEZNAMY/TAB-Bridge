import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("io.freefair.lombok") version "6.6.1"
    java
    `maven-publish`
}

group = "me.neznamy"
version = "6.0.0-SNAPSHOT"

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.0")
    compileOnly("org.spigotmc:spigot-api:1.20.2-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.2")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("io.freefair.gradle:lombok-plugin:6.6.1")
    implementation("org.bstats:bstats-bukkit:3.0.1")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
    shadowJar {
        archiveFileName.set("TAB-Bridge-${project.version}.jar")
        relocate("org.bstats", "me.neznamy.tab.bridge.libs.org.bstats")
    }
    build {
        dependsOn(shadowJar)
    }
    processResources {
        filesMatching("plugin.yml") {
            filter<ReplaceTokens>(mapOf("tokens" to mapOf("version" to project.version)))
        }
    }

    java{
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    }
}
