import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    java
    `maven-publish`
}

group = "me.neznamy"
version = "2.0.11"

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

dependencies {
    implementation(libs.annotations)
    compileOnly(libs.bukkit)
    compileOnly(libs.papi)
    compileOnly(libs.vault)
    compileOnly(libs.netty)
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
    }
    processResources {
        filesMatching("plugin.yml") {
            filter<ReplaceTokens>(mapOf("tokens" to mapOf("version" to project.version)))
        }
    }
}
