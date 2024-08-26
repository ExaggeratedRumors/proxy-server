plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "proxy-server"

include("client", "server")

project(":client").projectDir = File("client")
project(":server").projectDir = File("server")