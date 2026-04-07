plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ktomek.yamv.viewmodel"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

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
    api(project(":yamv"))
    implementation(project(":core"))
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.compose.hilt)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.preview)

    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.android.compiler)

    testImplementation(libs.bundles.testing.unit)
}
