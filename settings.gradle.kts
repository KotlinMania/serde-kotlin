pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins { kotlin("multiplatform") version "2.3.21" }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0" }

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "serde-kotlin"

fun findLocalDependencyDir(name: String): File? =
    listOf(
        file("../$name"),
        file("deps/$name"),
    ).firstOrNull { it.exists() }

val procMacro2Local = findLocalDependencyDir("proc-macro2-kotlin")
if (procMacro2Local != null) {
    includeBuild(procMacro2Local) {
        dependencySubstitution {
            substitute(module("io.github.kotlinmania:proc-macro2-kotlin")).using(project(":"))
        }
    }
}

val quoteLocal = findLocalDependencyDir("quote-kotlin")
if (quoteLocal != null) {
    includeBuild(quoteLocal) {
        dependencySubstitution {
            substitute(module("io.github.kotlinmania:quote-kotlin")).using(project(":"))
        }
    }
}

val synLocal = findLocalDependencyDir("syn-kotlin")
if (synLocal != null) {
    includeBuild(synLocal) {
        dependencySubstitution {
            substitute(module("io.github.kotlinmania:syn-kotlin")).using(project(":"))
        }
    }
}
