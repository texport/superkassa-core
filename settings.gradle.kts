pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version requireNotNull(
        Regex("""foojayResolver = "([^"]+)"""")
            .find(rootDir.resolve("gradle/libs.versions.toml").readText())
    ).groupValues[1]
}
rootProject.name = "superkassa-offline-queue"
