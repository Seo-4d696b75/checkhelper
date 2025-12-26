import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
    }
}

dependencies {
    implementation(platform(libs.kotlinx.coroutines.bom))
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.core)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.google.truth)
    testImplementation(libs.mockk)
}
