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

val procMacro2Local = file("../proc-macro2-kotlin")
if (procMacro2Local.exists()) {
    includeBuild(procMacro2Local) {
        dependencySubstitution {
            substitute(module("io.github.kotlinmania:proc-macro2-kotlin")).using(project(":"))
        }
    }
}

val quoteLocal = file("../quote-kotlin")
if (quoteLocal.exists()) {
    includeBuild(quoteLocal) {
        dependencySubstitution {
            substitute(module("io.github.kotlinmania:quote-kotlin")).using(project(":"))
        }
    }
}

val synLocal = file("../syn-kotlin")
if (synLocal.exists()) {
    includeBuild(synLocal) {
        dependencySubstitution {
            substitute(module("io.github.kotlinmania:syn-kotlin")).using(project(":"))
        }
    }
}
