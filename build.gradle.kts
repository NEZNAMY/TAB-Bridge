plugins {
    id("tab.parent")
}

allprojects {
    group = "me.neznamy"
    version = "6.0.4"
    description = "An addon to extend features with TAB on proxy"
}

val platforms = setOf(
    projects.bukkit,
    projects.bukkit.paper,
).map { it.dependencyProject }

val special = setOf(
    projects.shared
).map { it.dependencyProject }

subprojects {
    when (this) {
        in platforms -> plugins.apply("tab.platform-conventions")
        in special -> plugins.apply("tab.standard-conventions")
        else -> plugins.apply("tab.base-conventions")
    }
}