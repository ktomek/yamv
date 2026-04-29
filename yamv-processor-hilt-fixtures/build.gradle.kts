plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ktomek.yamv.processor.fixtures"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests.all { it.useJUnitPlatform() }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":yamv-core"))
    implementation(project(":yamv"))
    implementation(project(":yamv-hilt"))

    implementation(libs.dagger.hilt.android)
    implementation(libs.javax.inject)

    ksp(project(":yamv-processor-core"))
    ksp(project(":yamv-processor-hilt"))

    testImplementation(libs.bundles.testing.unit)
}
