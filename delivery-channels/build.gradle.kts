import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

// Каналы доставки чека покупателю: SMS, Telegram, WhatsApp и почта.
// Отдельно от модуля delivery: у каналов свои внешние зависимости (HTTP-клиент,
// почтовый клиент), а ядру и узлу, которым каналы не нужны, их тянуть незачем.
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
                // Платформенные мосты без ветвлений: журнал и движок HTTP.
                classes(
                    "io.github.texport.superkassa.delivery.channels.impl.common.PlatformJournal*",
                    "io.github.texport.superkassa.delivery.channels.impl.http.PlatformHttp*"
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
        namespace = "io.github.texport.superkassa.delivery.channels"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()

        withHostTest {}
    }
    iosArm64()
    iosX64()
    iosSimulatorArm64()

    sourceSets {
        // Почта и журнал на JVM и Android одни и те же: один исходник собирается в обе цели.
        jvmMain { kotlin.srcDir("src/jvmCommonMain/kotlin") }
        androidMain { kotlin.srcDir("src/jvmCommonMain/kotlin") }
        commonMain.dependencies {
            api(project(":delivery"))
            implementation(project(":core-string"))
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.angus.mail)
            implementation(libs.slf4j.api)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.angus.mail)
            implementation(libs.slf4j.api)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.ktor.client.mock)
        }
        jvmTest.dependencies {
            implementation(libs.greenmail)
        }
    }

    jvmToolchain(libs.versions.jvm.get().toInt())
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

tasks.named("check") {
    dependsOn("koverVerify")
}
