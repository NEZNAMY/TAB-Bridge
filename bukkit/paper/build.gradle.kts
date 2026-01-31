plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

val version = "1.21.7-R0.1-SNAPSHOT"

dependencies {
    implementation(projects.shared) // This should not be needed explicitly but somehow it is
    implementation(projects.bukkit)
    paperweight.paperDevBundle(version)
    compileOnly("io.papermc.paper:paper-api:${version}")
}

tasks.compileJava {
    options.release.set(21)
}