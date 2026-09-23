// Перенос данных узла в базу кассы в процессе приложения. Узел живёт только
// на настольных машинах, поэтому и модуль собирается только под JVM.
// Публикуется, как и остальные модули ядра: приложение зовёт перенос при запуске.
plugins {
    `maven-publish`
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    // Отдаёт публикации модуля корневой агрегации для Maven Central;
    // версия плагина задана в корне.
    id("com.gradleup.nmcp")
}

kotlin {
    jvm()

    jvmToolchain(libs.versions.jvm.get().toInt())

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":core-domain"))
            implementation(project(":core-database"))
            implementation(project(":offline-queue"))
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.slf4j.api)
        }
        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(project(":core-data"))
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
