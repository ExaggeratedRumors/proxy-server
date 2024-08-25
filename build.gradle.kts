plugins {
    kotlin("jvm") version "2.0.0"
}

group = "com.ertools"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    /** Reflection **/
    implementation(kotlin("reflect"))

    /** Serialization **/
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.+")

    /** Configuration **/
    implementation("com.typesafe:config:1.4.2")

    /** Test **/
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(20)
}