import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import java.security.MessageDigest
import java.io.FileInputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.plugins.signing.SigningExtension

plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.nmcp.aggregation)
    alias(libs.plugins.nmcp)
    `maven-publish`
    alias(libs.plugins.kover)
    alias(libs.plugins.ksp) apply false
}

group = "io.github.texport"
version = "1.5.0-SNAPSHOT"

dependencies {
    add("detektPlugins", libs.detekt.formatting)
    add("nmcpAggregation", project(":"))
}

allprojects {
    group = rootProject.group
    version = rootProject.version

    apply(plugin = "io.gitlab.arturbosch.detekt")

    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }

    dependencies {
        add("detektPlugins", rootProject.libs.detekt.formatting)
    }

    plugins.withId("io.gitlab.arturbosch.detekt") {
        configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
            config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
            buildUponDefaultConfig = true
            allRules = true
            autoCorrect = true
            ignoreFailures = true
            source.setFrom(
                files(
                    "src/commonMain/kotlin",
                    "src/jvmMain/kotlin",
                    "src/androidMain/kotlin",
                    "src/iosMain/kotlin"
                )
            )
        }
    }

    plugins.withType<MavenPublishPlugin> {
        configure<PublishingExtension> {
            publications.withType<MavenPublication>().configureEach {
                if (project != rootProject) {
                    artifactId = artifactId.replace(project.name, "superkassa-${project.name}")
                }
                val javadocJarTask = tasks.register<Jar>("${name}JavadocJar") {
                    description = "Generates Javadoc jar for publication ${this@configureEach.name}"
                    archiveClassifier.set("javadoc")
                    archiveAppendix.set(this@configureEach.name)
                }
                artifact(javadocJarTask)
                pom {
                    name.set(project.name)
                    description.set("Kotlin Multiplatform core module for Superkassa: ${project.name}")
                    url.set("https://github.com/texport/superkassa-core")
                    
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
                        connection.set("scm:git:git://github.com/texport/superkassa-core.git")
                        developerConnection.set("scm:git:ssh://github.com/texport/superkassa-core.git")
                        url.set("https://github.com/texport/superkassa-core")
                    }
                }
                
                // Remove local subproject dependencies from the generated POM
                pom.withXml {
                    val root = asNode()
                    val depsNode = root.children().firstOrNull { 
                        (it as? groovy.util.Node)?.name().toString().endsWith("dependencies") 
                    } as? groovy.util.Node
                    if (depsNode != null) {
                        val toRemove = mutableListOf<groovy.util.Node>()
                        for (child in depsNode.children()) {
                            if (child is groovy.util.Node && child.name().toString().endsWith("dependency")) {
                                val artifactIdNode = child.children().firstOrNull { 
                                    (it as? groovy.util.Node)?.name().toString().endsWith("artifactId") 
                                } as? groovy.util.Node
                                val artifactId = artifactIdNode?.value()?.toString() ?: ""
                                if (artifactId.contains("delivery") || artifactId.contains("queue") || 
                                    artifactId.contains("renderer") || artifactId.contains("domain") || 
                                    artifactId.contains("presentation") || artifactId.contains("data") || 
                                    artifactId.contains("string")
                                ) {
                                    toRemove.add(child)
                                }
                            }
                        }
                        toRemove.forEach { depsNode.remove(it) }
                    }
                }
            }
        }
        
        plugins.apply("signing")
        configure<SigningExtension> {
            val signingKey = System.getenv("SIGNING_KEY")
            val signingPassword = System.getenv("SIGNING_PASSWORD")
            if (!signingKey.isNullOrEmpty() && !signingPassword.isNullOrEmpty()) {
                useInMemoryPgpKeys(signingKey, signingPassword)
            }
            isRequired = false
            sign(extensions.getByType<PublishingExtension>().publications)
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile>().configureEach {
        if (name.contains("Test")) {
            enabled = false
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest>().configureEach {
        enabled = false
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}


kotlin {
    jvm()
    android {
        namespace = "io.github.texport.superkassa.core"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
        withHostTest {}
    }
    
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
            export(project(":core-data"))
            export(project(":core-string"))
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":core-domain"))
                api(project(":core-presentation"))
                api(project(":core-data"))
                api(project(":core-string"))
                api(project(":offline-queue"))
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.datetime)
            }
        }
        jvmMain {
            dependencies {
                api(libs.slf4j.api)
                api(libs.jakarta.validation)
                api(libs.swagger.annotations)
                api(libs.ofd.proto.codec)
                api(libs.ofd.network.client)
                api(project(":delivery"))
                // QR-код чека рисует receipt-renderer, чьи классы лежат в этом же jar.
                implementation(libs.zxing.core)
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
    
    // Встраиваемая сборка публикуется своим артефактом со своими зависимостями
    // (движок PDF, шрифты) и в общий jar ядра не входит. Перенос с узла нужен
    // только приложению при обновлении — узлу, который берёт этот jar, он чужой.
    subprojects.filter { it.name !in setOf("core-embedded", "core-import-node") }.forEach { sub ->
        dependsOn(sub.tasks.named("compileKotlinJvm"))
        val compileKotlin = sub.tasks.named("compileKotlinJvm", org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class)
        from(compileKotlin.map { it.destinationDirectory })

        dependsOn(sub.tasks.named("jvmProcessResources"))
        val processResources = sub.tasks.named("jvmProcessResources", Copy::class)
        from(processResources.map { it.destinationDir })
    }
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

nmcpAggregation {
    centralPortal {
        username.set(project.findProperty("ossrhUsername")?.toString() ?: System.getenv("OSSRH_USERNAME"))
        password.set(project.findProperty("ossrhPassword")?.toString() ?: System.getenv("OSSRH_PASSWORD"))
        publishingType.set("AUTOMATIC")
    }
}

nmcp {
    publishAllPublicationsToCentralPortal {
        username.set(project.findProperty("ossrhUsername")?.toString() ?: System.getenv("OSSRH_USERNAME"))
        password.set(project.findProperty("ossrhPassword")?.toString() ?: System.getenv("OSSRH_PASSWORD"))
        publishingType.set("AUTOMATIC")
    }
}

tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

tasks.register("copyResourcesToFrameworks") {
    description = "Copies receipt-renderer resources (CSS, templates, translations) into the built iOS frameworks"
    doLast {
        val buildDir = layout.buildDirectory.get().asFile
        val resourcesDir = project(":receipt-renderer").file("src/commonMain/resources")
        if (resourcesDir.exists()) {
            val configs = listOf("debug", "release")
            val targets = listOf("ios-arm64", "ios-arm64_x86_64-simulator")
            configs.forEach { config ->
                val xcframeworkDir = File(buildDir, "XCFrameworks/$config/SuperkassaCore.xcframework")
                if (xcframeworkDir.exists()) {
                    targets.forEach { target ->
                        val destFrameworkDir = File(xcframeworkDir, "$target/SuperkassaCore.framework")
                        if (destFrameworkDir.exists()) {
                            println("Copying receipt-renderer resources to ${destFrameworkDir.absolutePath}...")
                            resourcesDir.copyRecursively(destFrameworkDir, overwrite = true)
                        }
                    }
                }
            }
        }
    }
}

tasks.named("assembleSuperkassaCoreReleaseXCFramework") {
    finalizedBy("copyResourcesToFrameworks")
}

tasks.register("syncDebugToReleaseXCFramework") {
    doLast {
        val debugXc = file("build/XCFrameworks/debug/SuperkassaCore.xcframework")
        val releaseDir = file("build/XCFrameworks/release")
        if (debugXc.exists()) {
            releaseDir.mkdirs()
            debugXc.copyRecursively(File(releaseDir, "SuperkassaCore.xcframework"), overwrite = true)
            println("Synchronized debug SuperkassaCore.xcframework to release path!")
        }
    }
}

tasks.named("assembleSuperkassaCoreDebugXCFramework") {
    finalizedBy("copyResourcesToFrameworks", "syncDebugToReleaseXCFramework")
}

