plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
    jacoco
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()
    
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

val jacocoTestReport = tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("jvmTest"))
    classDirectories.setFrom(files(tasks.named("compileKotlinJvm")))
    sourceDirectories.setFrom(files("src/commonMain/kotlin", "src/jvmMain/kotlin"))
    executionData.setFrom(files(layout.buildDirectory.file("jacoco/jvmTest.exec")))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

val jacocoTestCoverageVerification = tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(jacocoTestReport)
    executionData.setFrom(files(layout.buildDirectory.file("jacoco/jvmTest.exec")))
    classDirectories.setFrom(files(tasks.named("compileKotlinJvm")))
    violationRules {
        rule {
            element = "CLASS"
            includes = listOf("kz.mybrain.superkassa.core.data.adapter.*")
            limit {
                minimum = "1.00".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(jacocoTestCoverageVerification)
}
