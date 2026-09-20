import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    `maven-publish`
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
}

room {
    schemaDirectory("$projectDir/schemas")
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
                    "io.github.texport.superkassa.coredatabase.impl.*",
                    "io.github.texport.superkassa.coredatabase.api.DatabaseBuilder*",
                    "io.github.texport.superkassa.coredatabase.impl.db.*"
                )
            }
        }

        verify {
            rule {
                bound {
                    coverageUnits = CoverageUnit.LINE
                    minValue = 40
                }
            }
        }
    }
}

kotlin {
    jvm()
    android {
        namespace = "io.github.texport.superkassa.coredatabase"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()

        withHostTest {}
    }
    iosArm64()
    iosX64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core-domain"))
            implementation(project(":offline-queue"))
            implementation(project(":core-string"))
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
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

dependencies {
    add("kspIosArm64", libs.room.compiler)
    add("kspIosX64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
    add("kspAndroid", libs.room.compiler)
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
