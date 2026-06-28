import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import java.security.MessageDigest
import java.io.FileInputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.multiplatform)
    jacoco
}

group = "kz.mybrain"
version = "1.0"

subprojects {
    group = rootProject.group
    version = rootProject.version
    repositories {
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        plugins.withId("io.gitlab.arturbosch.detekt") {
            add("detektPlugins", rootProject.libs.detekt.formatting)
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}


kotlin {
    jvm()
    
    iosArm64()
    iosX64()
    iosSimulatorArm64()

    jvmToolchain(libs.versions.jvm.get().toInt())

    val xcf = XCFramework("SuperkassaCore")
    listOf(iosArm64(), iosX64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "SuperkassaCore"
            xcf.add(this)
            export(project(":core-domain"))
            export(project(":core-presentation"))
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":core-domain"))
                api(project(":core-presentation"))
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.coroutines.core)
            }
        }
        jvmMain {
            dependencies {
                api(project(":core-data"))
                api(libs.slf4j.api)
                api(libs.superkassa.offline.queue)
                api(libs.jakarta.validation)
                api(libs.swagger.annotations)
                api(libs.ofd.proto.codec)
                api(libs.ofd.network.client)
                api(libs.superkassa.delivery)
                api(libs.resilience4j)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.archunit)
                implementation(kotlin("test"))
            }
        }
    }
}

tasks.named<Jar>("jvmJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    subprojects.forEach { sub ->
        dependsOn(sub.tasks.named("compileKotlinJvm"))
        val compileKotlin = sub.tasks.named("compileKotlinJvm", org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class)
        from(compileKotlin.map { it.destinationDirectory })
    }
}

jacoco {
    toolVersion = "0.8.12"
}

detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = true
    autoCorrect = true
    source.setFrom(files("core-domain/src/commonMain/kotlin", "core-presentation/src/commonMain/kotlin", "core-data/src/commonMain/kotlin"))
}

tasks.register("generateSpmManifest") {
    group = "publishing"
    description = "Zips SuperkassaCore XCFramework, calculates SHA-256 and writes Package.swift"
    dependsOn("assembleSuperkassaCoreReleaseXCFramework")

    doLast {
        val versionStr = project.version.toString()
        val repoUrl = "https://github.com/texport/superkassa-core"
        val zipName = "SuperkassaCore.xcframework.zip"
        val outputDir = layout.buildDirectory.dir("XCFrameworks/release").get().asFile
        val xcframeworkDir = File(outputDir, "SuperkassaCore.xcframework")
        val zipFile = File(outputDir, zipName)

        if (!xcframeworkDir.exists()) {
            throw GradleException("XCFramework not found at ${xcframeworkDir.absolutePath}")
        }

        // 1. Zipping XCFramework
        println("Zipping XCFramework to ${zipFile.absolutePath}...")
        zipFile.delete()
        ZipOutputStream(zipFile.outputStream().buffered()).use { zos ->
            xcframeworkDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.relativeTo(xcframeworkDir.parentFile).path
                    zos.putNextEntry(ZipEntry(relativePath))
                    file.inputStream().buffered().use { input ->
                        input.copyTo(zos)
                    }
                    zos.closeEntry()
                }
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
        val packageSwiftFile = rootProject.file("Package.swift")
        println("Writing Package.swift to ${packageSwiftFile.absolutePath}...")
        packageSwiftFile.writeText(
            """
            // swift-tools-version:5.5
            import PackageDescription

            let package = Package(
                name: "SuperkassaCore",
                platforms: [
                    .iOS(.v15)
                ],
                products: [
                    .library(
                        name: "SuperkassaCore",
                        targets: ["SuperkassaCore"]
                    ),
                ],
                dependencies: [],
                targets: [
                    .binaryTarget(
                        name: "SuperkassaCore",
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
