plugins {
    id("tab.standard-conventions")
    id("com.github.johnrengelman.shadow")
}

tasks {
    shadowJar {
        archiveFileName.set("TAB-Bridge-${project.name}-${project.version}.jar")
        relocate("org.bstats", "me.neznamy.tab.libs.org.bstats")
    }
}
