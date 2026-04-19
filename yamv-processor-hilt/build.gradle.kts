import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-library")
    kotlin("jvm")
    alias(libs.plugins.ksp)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":yamv-core"))
    implementation(project(":yamv-processor-core"))
    implementation(libs.ksp.api)

    annotationProcessor(libs.auto.service)
    implementation(libs.javapoet)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.javax.annotation.api)
}
