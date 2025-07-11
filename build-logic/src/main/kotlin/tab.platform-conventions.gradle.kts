plugins {
    id("tab.standard-conventions")
    id("com.gradleup.shadow")
}

tasks {
    shadowJar {
        archiveFileName.set("TAB-Bridge-${project.name}-${project.version}.jar")
        relocate("org.bstats", "me.neznamy.tab.bridge.libs.org.bstats")
    }
}
