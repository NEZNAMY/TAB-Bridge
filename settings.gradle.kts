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
        maven("https://maven.architectury.dev/")
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "TAB-Bridge"

include(":shared")
include(":bukkit")
include(":bukkit:paper")
include(":fabric")
include(":fabric:v1_18_2")
include(":fabric:v1_19_4")
include(":fabric:v1_20_4")
include(":jar")

