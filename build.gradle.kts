plugins {
    kotlin("jvm") version "2.0.0"
}

group = "com.ertools"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

allprojects {
    repositories {
        mavenCentral()
        google()
    }
}

subprojects {
    apply(plugin = "kotlin")

    dependencies {
        /** Reflection **/
        implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.20")
        implementation(kotlin("reflect"))

        /** Serialization **/
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.+")

        /** Configuration **/
        implementation("com.typesafe:config:1.4.2")

        /** Test **/
        testImplementation(kotlin("test"))
    }
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(20)
}

tasks.register("runServer") {
    group = "application"
    description = "Run server"
    dependsOn(":server:run")
}

tasks.register("runClient") {
    group = "application"
    description = "Run client"
    dependsOn("client:run")
    shouldRunAfter("runServer")
}