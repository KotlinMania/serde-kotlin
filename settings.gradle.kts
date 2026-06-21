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

// Multi-build hierarchy mirroring Rust's crate split (serde_core / serde / serde_derive),
// following the km-io / kotlinx-io convention-plugin layout.
include(":serde-core")
include(":serde")
include(":serde-derive")

project(":serde-core").projectDir = file("./serde_core")
project(":serde").projectDir = file("./serde")
project(":serde-derive").projectDir = file("./serde_derive")
