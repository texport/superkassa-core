import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    `maven-publish`
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kover)
}

repositories {
    google()
    mavenCentral()
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "io.github.texport.superkassa.offline_queue.application.logging.*"
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
        namespace = "io.github.texport.superkassa.offline_queue"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()

        withHostTest {}
    }
    iosArm64()
    iosX64()
    iosSimulatorArm64()


    sourceSets {
        commonMain.dependencies {
            implementation(project(":core-string"))
        }
        jvmMain.dependencies {
            implementation(libs.slf4j.api)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }

    jvmToolchain(libs.versions.jvm.get().toInt())
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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

tasks.named("check") {
    dependsOn("koverVerify")
}
