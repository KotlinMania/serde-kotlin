pluginManagement {
    includeBuild("build-logic")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

val localMavenValidation =
    providers.gradleProperty("project.repositories.mavenLocal")
        .map { it.trim().toBooleanStrict() }
        .getOrElse(false)

dependencyResolutionManagement {
    if (localMavenValidation) {
        val localMavenPath = providers.systemProperty("maven.repo.local").orNull
        require(localMavenPath != null && File(localMavenPath).isAbsolute) {
            "Local Maven validation requires -Dmaven.repo.local=/absolute/path/to/isolated-repository"
        }
        // Kotlin tool setup temporarily adds and removes its Node/Yarn Ivy repositories.
        repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    }
    repositories {
        if (localMavenValidation) {
            exclusiveContent {
                forRepository {
                    mavenLocal { name = "kotlinManiaLocalValidation" }
                }
                filter { includeGroup("io.github.kotlinmania") }
            }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "serde-kotlin"

// Multi-build hierarchy mirroring Rust's crate split (serde_core / serde / serde_derive),
// following the km-io / kotlinx-io convention-plugin layout.
include(":serde-core")
include(":serde-derive")

project(":serde-core").projectDir = file("./serde_core")
project(":serde-derive").projectDir = file("./serde_derive")
