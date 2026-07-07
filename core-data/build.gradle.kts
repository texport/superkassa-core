import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

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

kotlin {
    jvm()
    android {
        namespace = "kz.mybrain.superkassa.core.data"
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
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
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
                implementation(libs.ofd.proto.codec)
                implementation(libs.ofd.network.client)
                implementation(libs.superkassa.offline.queue)
                implementation(libs.superkassa.delivery)
                implementation(libs.resilience4j)
            }
        }
        androidMain {
            kotlin.srcDirs("src/jvmMain/kotlin")
            dependencies {
                implementation(libs.slf4j.api)
                implementation(libs.ofd.proto.codec)
                implementation(libs.ofd.network.client)
                implementation(libs.superkassa.offline.queue)
                implementation(libs.superkassa.delivery)
                implementation(libs.resilience4j)
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
                classes("kz.mybrain.superkassa.core.data.ofd.*")
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
