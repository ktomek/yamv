import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-library")
    kotlin("jvm")
    alias(libs.plugins.ksp)
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

// jar {
//    from sourceSets.main.output
//    manifest {
//        attributes(
//                'Class-Path': configurations.runtimeClasspath.collect { it.getName() }.join(' '),
//                'AutoService': 'com.ktomek.yamv.processor.ReducerProcessor'
//        )
//    }
// Include all your processor's classes and resources in the JAR
//    from {
//        (configurations.runtimeClasspath).collect {
//            it.isDirectory() ? it : zipTree(it)
//        }
//    }
// }

dependencies {
    implementation(project(":core"))
    implementation(project(":annotations"))
    implementation(libs.ksp.api)

    annotationProcessor(libs.auto.service)
    implementation(libs.javapoet)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.javax.annotation.api)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
            groupId = "com.ktomek.yamv" // Replace with your group ID
            artifactId = project.name
            version = "0.0.1" // Update version as needed

            // Specify which parts to include in the publication
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
