pluginManagement {
    // kotlinmania:maven-mode:v1 begin
    val mavenProperties =
        java.util.Properties().apply {
            val source = settingsDir.resolve("maven.properties")
            require(source.isFile) { "Missing Maven mode configuration: $source" }
            source.inputStream().use { load(it) }
        }
    val localMaven = mavenProperties.getProperty("maven.local")?.trim()?.toBooleanStrictOrNull()
    require(localMaven != null) { "maven.properties must set maven.local=true or false" }
    val localMavenValidation =
        providers
            .gradleProperty("project.repositories.mavenLocal")
            .map { it.trim().toBooleanStrict() }
            .getOrElse(false)
    val configuredLocalRepository =
        mavenProperties
            .getProperty("maven.localRepository")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { File(it) }
            ?: File(System.getProperty("user.home"), ".m2/repository")
    require(configuredLocalRepository.isAbsolute) { "maven.localRepository must be an absolute path" }
    val localRepository =
        if (localMavenValidation) {
            val requested = providers.systemProperty("maven.repo.local").orNull
            require(requested != null && File(requested).isAbsolute) {
                "Local Maven validation requires -Dmaven.repo.local=/absolute/path/to/isolated-repository"
            }
            File(requested)
        } else {
            configuredLocalRepository
        }
    // Ordinary mode comes from the file. Explicit validation retains its isolated command-line path.
    System.setProperty("maven.repo.local", localRepository.absolutePath)
    settings.extra["mavenLocalEnabled"] = localMaven
    settings.extra["mavenLocalValidation"] = localMavenValidation
    // kotlinmania:maven-mode:v1 end

    includeBuild("build-logic")

    repositories {
        if (localMaven && !localMavenValidation) mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

val localMavenEnabled = extra["mavenLocalEnabled"] as Boolean
val localMavenValidation = extra["mavenLocalValidation"] as Boolean

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
        if (!localMavenValidation && localMavenEnabled) mavenLocal()
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

// kotlinmania:maven-project-repositories:v1 begin
gradle.beforeProject {
    repositories {
        if (localMavenValidation) {
            exclusiveContent {
                forRepository { mavenLocal { name = "kotlinManiaLocalValidation" } }
                filter { includeGroup("io.github.kotlinmania") }
            }
        } else if (localMavenEnabled) {
            mavenLocal()
        }
        google()
        mavenCentral()
    }
    buildscript.repositories {
        if (localMavenValidation) {
            exclusiveContent {
                forRepository { mavenLocal { name = "kotlinManiaLocalValidation" } }
                filter { includeGroup("io.github.kotlinmania") }
            }
        } else if (localMavenEnabled) {
            mavenLocal()
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
// kotlinmania:maven-project-repositories:v1 end
