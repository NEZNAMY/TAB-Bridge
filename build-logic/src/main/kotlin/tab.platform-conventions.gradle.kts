plugins {
    id("tab.standard-conventions")
    id("com.gradleup.shadow")
}

tasks.withType<Zip>().configureEach {
    isZip64 = true // To avoid compilation error "org.apache.tools.zip.Zip64RequiredException: archive contains more than 65535 entries."
}

tasks {
    shadowJar {
        archiveFileName.set("TAB-Bridge-${project.name}-${project.version}.jar")
        relocate("org.bstats", "me.neznamy.tab.bridge.libs.org.bstats")
    }
}
