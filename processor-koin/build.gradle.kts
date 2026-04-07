import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-library")
    kotlin("jvm")
    `maven-publish`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":processor-core"))
    implementation(libs.ksp.api)

    annotationProcessor(libs.auto.service)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.javax.annotation.api)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
            groupId = "com.ktomek.yamv"
            artifactId = project.name
            version = "0.0.1"

            pom {
                name.set(project.name)
                description.set("Description of ${project.name} module")
                url.set("https://github.com/ktomek/yamv")

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("ktomek")
                        name.set("Tomasz Kaszkowiak")
                    }
                }

                scm {
                    connection.set("scm:git:github.com/ktomek/yamv.git")
                    developerConnection.set("scm:git:ssh://github.com/ktomek/yamv.git")
                    url.set("https://github.com/ktomek/yamv")
                }
            }
        }
    }
}
