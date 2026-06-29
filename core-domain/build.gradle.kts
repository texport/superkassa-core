plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
    jacoco
}

repositories {
    mavenLocal()
    mavenCentral()
}

val conflictingOfflineQueue = configurations.create("conflictingOfflineQueue")

dependencies {
    conflictingOfflineQueue(libs.superkassa.offline.queue)
}

val cleanOfflineQueueJar = tasks.register<Jar>("cleanOfflineQueueJar") {
    archiveClassifier.set("clean")
    val inputJar = conflictingOfflineQueue.elements.map { it.first().asFile }
    from(inputJar.map { zipTree(it) }) {
        exclude("kz/mybrain/superkassa/core/domain/**")
    }
}

kotlin {
    jvm()
    
    iosArm64()
    iosX64()
    iosSimulatorArm64()

    jvmToolchain(libs.versions.jvm.get().toInt())

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        jvmMain {
            dependencies {
                implementation(libs.slf4j.api)
                implementation(libs.jakarta.validation)
                implementation(files(cleanOfflineQueueJar))
                compileOnly(libs.superkassa.offline.queue)
            }
        }
        jvmTest {
            dependencies {
                implementation(kotlin("reflect"))
                implementation(libs.mockk)
            }
        }
    }

    targets.all {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnit()
    maxHeapSize = "2048m"
}

val jacocoTestReport = tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("jvmTest"))
    classDirectories.setFrom(files(tasks.named("compileKotlinJvm")))
    sourceDirectories.setFrom(files("src/commonMain/kotlin", "src/jvmMain/kotlin"))
    executionData.setFrom(files(layout.buildDirectory.file("jacoco/jvmTest.exec")))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
