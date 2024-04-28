import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("io.freefair.lombok") version "6.6.1"
    java
    `maven-publish`
}

group = "me.neznamy"
version = "5.0.6"

dependencies {
    compileOnly(libs.annotations)
    compileOnly(libs.bukkit)
    compileOnly(libs.papi)
    compileOnly(libs.vault)
    compileOnly(libs.netty)
    compileOnly("net.luckperms:api:5.4")
    compileOnly("net.kyori:adventure-api:4.13.0")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.13.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.13.0")
    compileOnly(libs.plugin.lombok)
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
