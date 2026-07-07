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
        namespace = "kz.mybrain.superkassa.core"
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
                implementation(libs.swagger.annotations)
            }
        }
        androidMain {
            kotlin.srcDirs("src/jvmMain/kotlin")
            dependencies {
                implementation(libs.slf4j.api)
                implementation(libs.jakarta.validation)
                implementation(libs.swagger.annotations)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.mockk)
            }
        }
        named("androidHostTest") {
            dependencies {
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

kover {
    reports {
        filters {
            excludes {
                classes(
                    "kz.mybrain.superkassa.core.presentation.model.*"
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


