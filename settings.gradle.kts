pluginManagement {
    includeBuild("build-logic")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "serde-kotlin"

include(":serde-derive")
include(":serde-core")
include(":serde")

project(":serde-derive").projectDir = file("./serde_derive")
project(":serde-core").projectDir = file("./serde_core")
project(":serde").projectDir = file("./serde")
