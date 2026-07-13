import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
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

kotlin {
    jvm()
    android {
        namespace = "io.github.texport.superkassa.core.data"
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
                implementation(project(":core-domain"))
                implementation(project(":core-presentation"))
                implementation(project(":core-string"))
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(project(":offline-queue"))
                implementation(project(":delivery"))
                implementation(project(":receipt-renderer"))
                implementation(libs.ofd.proto.codec)
                implementation(libs.ofd.network.client)
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
            }
        }
        androidMain {

            dependencies {
                implementation(libs.slf4j.api)
            }
        }
        jvmTest {
            dependencies {
                implementation(project(":core-presentation"))
                implementation(libs.archunit)
                implementation(kotlin("test"))
                implementation(libs.mockk)
                implementation(libs.ofd.kt.proto)
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

kover {
    reports {
        filters {
            excludes {
                classes("io.github.texport.superkassa.core.data.impl.ofd.*")
                classes("io.github.texport.superkassa.core.data.impl.util.*")
                classes("io.github.texport.superkassa.core.data.api.*")
            }
        }
        verify {
            rule {
                bound {
                    coverageUnits = CoverageUnit.LINE
                    minValue = 100
                }
            }
        }
    }
}
