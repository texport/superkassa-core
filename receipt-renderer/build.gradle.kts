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
        verify {
            rule {
                bound {
                    coverageUnits = CoverageUnit.INSTRUCTION
                    minValue = 90
                }
                bound {
                    coverageUnits = CoverageUnit.BRANCH
                    minValue = 80
                }
                bound {
                    coverageUnits = CoverageUnit.LINE
                    minValue = 95
                }
            }
        }
    }
}

kotlin {
    jvm()
    android {
        namespace = "io.github.texport.superkassa.receiptrenderer"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()

        withHostTest {}
    }
    iosArm64()
    iosX64()
    iosSimulatorArm64()


    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":core-domain"))
                implementation(project(":core-string"))
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
            }
        }
        jvmMain {
            dependencies {
                implementation(libs.slf4j.api)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.mockk)
            }
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


