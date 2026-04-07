import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("multiplatform")
    `maven-publish`
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            kotlin.srcDirs("src/main/kotlin")
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }
    }

    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.ktomek.yamv"
            artifactId = project.name
            version = "0.0.1"
        }
    }
}
