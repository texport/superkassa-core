import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import java.security.MessageDigest
import java.io.FileInputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.nmcp)
    alias(libs.plugins.nmcp.aggregation)
    `maven-publish`
    signing
}

group = "io.github.texport"
version = "1.0.4"

repositories {
    google()
    mavenCentral()
}

dependencies {
    detektPlugins(libs.detekt.formatting)
    add("nmcpAggregation", dependencies.project(mapOf("path" to ":")))
}

detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = true
    autoCorrect = true
    source.setFrom(files(
        "src/commonMain/kotlin",
        "src/jvmMain/kotlin",
        "src/androidMain/kotlin",
        "src/iosMain/kotlin"
    ))
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "kz.mybrain.superkassa.offline_queue.application.logging.*"
                )
            }
        }

        verify {
            rule {
                bound {
                    coverageUnits = CoverageUnit.INSTRUCTION
                    minValue = 90
                }
                bound {
                    coverageUnits = CoverageUnit.BRANCH
                    minValue = 94
                }
                bound {
                    coverageUnits = CoverageUnit.LINE
                    minValue = 99
                }
            }
        }
    }
}

kotlin {
    jvm()
    android {
        namespace = "kz.mybrain.superkassa.offline_queue"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()

        withHostTest {}
    }
    
    val xcf = XCFramework("SuperkassaOfflineQueue")
    listOf(iosArm64(), iosX64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "SuperkassaOfflineQueue"
            xcf.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Core offline-queue logic has no external dependencies
        }
        jvmMain.dependencies {
            implementation(libs.slf4j.api)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }

    jvmToolchain(libs.versions.javaTargetCore.get().toInt())
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<Javadoc>().configureEach {
    options {
        encoding = "UTF-8"
        if (this is StandardJavadocDocletOptions) {
            addStringOption("Xdoclint:none", "-quiet")
        }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        val javadocJarTask = tasks.register<Jar>("${name}JavadocJar") {
            description = "Generates Javadoc jar for publication ${this@configureEach.name}"
            archiveClassifier.set("javadoc")
            archiveAppendix.set(this@configureEach.name)
        }
        artifact(javadocJarTask)
        pom {
            name.set("superkassa-offline-queue")
            description.set("Offline command queue and synchronization logic for Superkassa")
            url.set("https://github.com/texport/superkassa-offline-queue")
            
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            
            developers {
                developer {
                    id.set("sergeyivanov")
                    name.set("Sergey Ivanov")
                    email.set("ivanov.sergey.ekb@gmail.com")
                }
            }
            
            scm {
                connection.set("scm:git:git://github.com/texport/superkassa-offline-queue.git")
                developerConnection.set("scm:git:ssh://github.com/texport/superkassa-offline-queue.git")
                url.set("https://github.com/texport/superkassa-offline-queue")
            }
        }
    }
}

signing {
    isRequired = false
    sign(publishing.publications)
}

nmcpAggregation {
    centralPortal {
        username.set(project.findProperty("ossrhUsername")?.toString() ?: System.getenv("OSSRH_USERNAME"))
        password.set(project.findProperty("ossrhPassword")?.toString() ?: System.getenv("OSSRH_PASSWORD"))
        publishingType.set("USER_MANAGED")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

tasks.named("check") {
    dependsOn("koverVerify")
}

tasks.register("generateSpmManifest") {
    group = "publishing"
    description = "Zips SuperkassaOfflineQueue XCFramework, calculates SHA-256 and writes Package.swift"
    dependsOn("assembleSuperkassaOfflineQueueReleaseXCFramework")

    val versionStr = project.version.toString()
    val outputDir = layout.buildDirectory.dir("XCFrameworks/release").get().asFile
    val packageSwiftFile = rootProject.file("Package.swift")

    doLast {
        val repoUrl = "https://github.com/texport/superkassa-offline-queue"
        val zipName = "SuperkassaOfflineQueue.xcframework.zip"
        val xcframeworkDir = File(outputDir, "SuperkassaOfflineQueue.xcframework")
        val zipFile = File(outputDir, zipName)

        if (!xcframeworkDir.exists()) {
            throw GradleException("XCFramework not found at ${xcframeworkDir.absolutePath}")
        }

        // 1. Zipping XCFramework
        println("Zipping XCFramework to ${zipFile.absolutePath}...")
        zipFile.delete()
        ZipOutputStream(zipFile.outputStream().buffered()).use { zos ->
            xcframeworkDir.walkTopDown()
                .filter { it.isFile }
                .sortedBy { it.relativeTo(xcframeworkDir.parentFile).invariantSeparatorsPath }
                .forEach { file ->
                    val relativePath = file.relativeTo(xcframeworkDir.parentFile).invariantSeparatorsPath
                    val entry = ZipEntry(relativePath).apply {
                        time = 0L
                    }
                    zos.putNextEntry(entry)
                    file.inputStream().buffered().use { input ->
                        input.copyTo(zos)
                    }
                    zos.closeEntry()
                }
        }

        // 2. Compute SHA-256
        println("Computing SHA-256 checksum...")
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(zipFile).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead = fis.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = fis.read(buffer)
            }
        }
        val checksumBytes = digest.digest()
        val checksum = checksumBytes.joinToString("") { "%02x".format(it) }
        println("SHA-256: $checksum")

        // 3. Write Package.swift
        println("Writing Package.swift to ${packageSwiftFile.absolutePath}...")
        packageSwiftFile.writeText(
            """
            // swift-tools-version:5.5
            import PackageDescription

            let package = Package(
                name: "SuperkassaOfflineQueue",
                platforms: [
                    .iOS(.v15)
                ],
                products: [
                    .library(
                        name: "SuperkassaOfflineQueue",
                        targets: ["SuperkassaOfflineQueue"]
                    ),
                ],
                dependencies: [],
                targets: [
                    .binaryTarget(
                        name: "SuperkassaOfflineQueue",
                        url: "$repoUrl/releases/download/v$versionStr/$zipName",
                        checksum: "$checksum"
                    )
                ]
            )
            """.trimIndent() + "\n"
        )
        println("SPM manifest generation complete for version $versionStr!")
    }
}
