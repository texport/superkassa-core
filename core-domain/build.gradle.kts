plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
    alias(libs.plugins.kover)
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
    description = "Packs offline queue jar without domain classes."
    archiveClassifier.set("clean")
    val inputJar = conflictingOfflineQueue.elements.map { it.first().asFile }
    from(inputJar.map { zipTree(it) }) {
        exclude("kz/mybrain/superkassa/core/domain/**")
    }
}

kotlin {
    jvm()
    android {
        namespace = "kz.mybrain.superkassa.core.domain"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
        withHostTest {}
    }
    
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
            kotlin.srcDirs("src/jvmMain/kotlin", "src/jvmOnly/kotlin")
            dependencies {
                implementation(libs.slf4j.api)
                implementation(libs.jakarta.validation)
                implementation(files(cleanOfflineQueueJar))
                compileOnly(libs.superkassa.offline.queue)
            }
        }
        androidMain {
            kotlin.srcDirs("src/jvmMain/kotlin")
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
        named("androidHostTest") {
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

kover {
    reports {
        filters {
            excludes {
                classes(
                    "kz.mybrain.superkassa.core.domain.model.*",
                    "kz.mybrain.superkassa.core.domain.exception.*",
                    "kz.mybrain.superkassa.core.domain.logging.*",
                    "kz.mybrain.superkassa.core.domain.port.*",
                    "kz.mybrain.superkassa.core.domain.helper.common.*",
                    "kz.mybrain.superkassa.core.domain.helper.zxreport.*",
                    "kz.mybrain.superkassa.core.domain.helper.KkmCommonHelper",
                    "kz.mybrain.superkassa.core.domain.helper.OfdResponseParser*",
                    "kz.mybrain.superkassa.core.domain.helper.ReceiptDeliveryHelper",
                    "kz.mybrain.superkassa.core.domain.usecase.counter.*",
                    "kz.mybrain.superkassa.core.domain.usecase.kkm.*",
                    "kz.mybrain.superkassa.core.domain.usecase.ofd.*",
                    "kz.mybrain.superkassa.core.domain.usecase.print.*",
                    "kz.mybrain.superkassa.core.domain.usecase.queue.*",
                    "kz.mybrain.superkassa.core.domain.usecase.shift.*",
                    "kz.mybrain.superkassa.core.domain.validation.*"
                )
            }
        }
        verify {
            rule {
                bound {
                    coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.LINE
                    minValue = 100
                }
            }
        }
    }
}


