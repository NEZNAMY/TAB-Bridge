plugins {
    `java-library`
    id("io.freefair.lombok")
}

dependencies {
    implementation(projects.shared)
    implementation("org.bstats:bstats-bukkit:3.1.0")
    compileOnly("org.spigotmc:spigot-api:1.20.2-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("LibsDisguises:LibsDisguises:10.0.21") {
        exclude("org.spigotmc", "spigot")
        exclude("org.spigotmc", "spigot-api")
        exclude("com.github.dmulloy2", "ProtocolLib")
        exclude("org.ow2.asm", "asm")
        exclude("net.md-5", "bungeecord-chat")
    }
}