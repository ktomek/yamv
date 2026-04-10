plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}

android {
    namespace = "com.ktomek.yamv.hilt"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    api(project(":yamv-retainer"))
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.android.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.ktomek.yamv"
            artifactId = project.name
            version = rootProject.version.toString()
        }
    }
}
