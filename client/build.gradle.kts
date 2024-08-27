plugins {
    kotlin("jvm") version "2.0.0"
}

tasks {
    val run by registering(JavaExec::class) {
        group = "application"
        description = "Run client"
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass = "ClientMainKt"
    }
}

dependencies {
    implementation(project(":commons"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1-Beta")
}