import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

// Оснастка проверок для потребителей ядра: БФД внутри процесса и готовая
// касса в каталоге данных. Приложение и узел берут её только в тестовые
// зависимости, поэтому в их выпуск она не попадает.
plugins {
    `maven-publish`
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kover)
}

kotlin {
    jvm()
    android {
        namespace = "io.github.texport.superkassa.testing"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }

    jvmToolchain(libs.versions.jvm.get().toInt())

    sourceSets {
        // На JVM и Android оснастка одна и та же: один исходник собирается в обе цели.
        jvmMain { kotlin.srcDir("src/jvmCommonMain/kotlin") }
        androidMain { kotlin.srcDir("src/jvmCommonMain/kotlin") }
        commonMain.dependencies {
            api(project(":core-embedded"))
            api(libs.ofd.network.client)
            // Запросы кассы проверка читает разобранными — типами протокола.
            api(libs.ofd.kt.proto)
            implementation(project(":core-data"))
            implementation(libs.kotlinx.coroutines.core)
        }
        jvmTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

kover {
    reports {
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
