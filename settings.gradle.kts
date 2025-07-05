enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositories {
        mavenCentral() // LuckPerms, bStats
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // Spigot API
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PlaceholderAPI
        maven("https://jitpack.io") // Vault
        maven("https://repo.md-5.net/content/groups/public/") // LibsDisguises
        maven("https://repo.papermc.io/repository/maven-public/") // paperweight
    }
}

pluginManagement {
    includeBuild("build-logic")
    repositories {
        maven("https://repo.spongepowered.org/repository/maven-public/")
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "TAB-Bridge"

include("shared")
include("bukkit")
include("bukkit:paper")
include("jar")