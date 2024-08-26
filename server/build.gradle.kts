plugins {
    kotlin("jvm") version "2.0.0"
}

tasks {
    val run by registering(JavaExec::class) {
        group = "application"
        description = "Run server"
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass = "ServerMainKt"
    }
}