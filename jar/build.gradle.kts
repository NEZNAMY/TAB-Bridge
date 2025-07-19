import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow")
}

val platforms = setOf(
    rootProject.projects.bukkit,
    rootProject.projects.bukkit.paper,
).map { it.dependencyProject }

val moddedPlatforms = setOf(
    rootProject.projects.fabric,
    rootProject.projects.fabric.v1182,
    rootProject.projects.fabric.v1194,
    rootProject.projects.fabric.v1204,
).map { it.dependencyProject }

tasks {
    shadowJar {
        archiveFileName.set("TAB-Bridge-${project.version}.jar")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        fun registerPlatform(project: Project, shadeTask: org.gradle.jvm.tasks.Jar) {
            dependsOn(shadeTask)
            dependsOn(project.tasks.withType<Jar>())
            from(zipTree(shadeTask.archiveFile))
        }

        platforms.forEach {
            registerPlatform(it, it.tasks.named<ShadowJar>("shadowJar").get())
        }

        moddedPlatforms.forEach {
            registerPlatform(it, it.tasks.named<org.gradle.jvm.tasks.Jar>("remapJar").get())
        }
    }
    build {
        dependsOn(shadowJar)
    }
}