plugins {
    kotlin("multiplatform")
    id("com.android.library")
    `maven-publish`
}

android {
    namespace = "com.ktomek.yamv.viewmodel"
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
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":yamv"))
            implementation(project(":core"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.lifecycle.viewmodel)
        }
        androidMain.dependencies {
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.viewmodel.savedstate)
            implementation(libs.androidx.compose.ui)
            implementation(libs.androidx.compose.preview)
        }
        androidUnitTest.dependencies {
            implementation(libs.bundles.testing.unit)
        }
    }
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
