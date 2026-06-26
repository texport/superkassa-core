plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    jacoco
}

group = "kz.mybrain"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
    google()
}



dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.slf4j.api)
    implementation(libs.ofd.proto.codec)
    implementation(libs.ofd.network.client)
    implementation(libs.superkassa.offline.queue)
    implementation(libs.superkassa.delivery)
    api(libs.superkassa.time.java)
    
    implementation(libs.resilience4j)
    implementation(libs.jakarta.validation)
    implementation(libs.swagger.annotations)
    
    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
    testImplementation(libs.ofd.kt.proto)
    detektPlugins(libs.detekt.formatting)
}

kotlin {
    jvmToolchain(17)
}


detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = true
    autoCorrect = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "17"
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                minimum = "0.44".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

