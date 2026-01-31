plugins {
    id("tab.parent")
}

allprojects {
    group = "me.neznamy"
    version = "6.2.0"
    description = "An addon to extend features with TAB on proxy"
}

val platforms = setOf(
    ":bukkit",
    ":bukkit:paper",
    ":fabric"
)
val special = setOf(
    ":shared"
)

subprojects {
    when (path) {
        in platforms -> plugins.apply("tab.platform-conventions")
        in special -> plugins.apply("tab.standard-conventions")
        else -> plugins.apply("tab.base-conventions")
    }
}