plugins {
    `maven-publish`
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

repositories {
    mavenLocal()
    google()
    mavenCentral()
}

kotlin {
    jvm()
    android {
        namespace = "io.github.texport.superkassa.embedded"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }
    iosArm64()
    iosX64()
    iosSimulatorArm64()

    jvmToolchain(libs.versions.jvm.get().toInt())

    sourceSets {
        commonMain.dependencies {
            api(project(":core-presentation"))
            api(project(":core-domain"))
            implementation(project(":core-data"))
            implementation(project(":core-database"))
            api(project(":delivery"))
            implementation(project(":receipt-renderer"))
            implementation(project(":offline-queue"))
            implementation(project(":core-string"))
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ofd.network.client)
        }
        // Файлы, сокет и потоки на JVM и Android одни и те же: один исходник
        // собирается в обе цели, вместо двух копий.
        jvmMain { kotlin.srcDir("src/jvmCommonMain/kotlin") }
        androidMain { kotlin.srcDir("src/jvmCommonMain/kotlin") }
        jvmMain.dependencies {
            // Типы запросов фасада несут аннотации проверки: без них у приложения
            // не выводится тип списка позиций чека.
            api(libs.jakarta.validation)
            implementation(libs.openhtmltopdf.pdfbox)
            implementation(libs.pdfbox)
            implementation(libs.jsoup)
            implementation(libs.dejavu.fonts)
        }
        androidMain.dependencies {
            api(libs.jakarta.validation)
        }
        jvmTest.dependencies {
            implementation(kotlin("test"))
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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
