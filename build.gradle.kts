plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "kz.mybrain"
version = "1.0"

repositories {
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
    
    implementation(libs.resilience4j)
    implementation(libs.jakarta.validation)
    implementation(libs.swagger.annotations)
    
    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
    testImplementation(libs.ofd.kt.proto)
}

kotlin {
    jvmToolchain(17)
}


detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "17"
}
