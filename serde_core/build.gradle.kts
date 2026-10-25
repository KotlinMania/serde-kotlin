// serde_core — the serde_core analogue: trait/data-model definitions, no derive.
// Slimmed to the shared convention plugins (build-logic); module-specific config only.
// This module publishes separately as io.github.kotlinmania:serde-core.

plugins {
    id("kotlinx-io-multiplatform")
    id("kotlinx-io-publish")
    id("kotlinx-io-dokka")
    id("kotlinx-io-android-compat")
    id("kotlinx-io-compatibility")
    alias(libs.plugins.kover)
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation("io.github.kotlinmania:num-traits-kotlin:0.2.15")
    }
    sourceSets {
        commonMain {
            // Existing kernel 300f797 sources; compile the three files together.
            kotlin.source(project.objects.sourceDirectorySet("rustUtf8Cow", "Verified Native UTF-8 Cow sources").apply {
                srcDir(rootProject.layout.projectDirectory.dir("../rust-lang-kotlin/candidates/core/kotlin"))
                srcDir(rootProject.layout.projectDirectory.dir("../rust-lang-kotlin/candidates/alloc/kotlin"))
                include("org/rustlang/core/slice/SharedByteSlice.kt")
                include("org/rustlang/core/str/Utf8ByteOperations.kt")
                include("org/rustlang/alloc/borrow/Utf8Cow.kt")
            })
        }
    }
    android {
        namespace = "io.github.kotlinmania.serdecore"
    }
}

// Android flattens nested source directories, so retain the same exact-file
// boundary at Kotlin JVM/Android compilation as at common/Native compilation.
val verifiedRustUtf8CowRoot = rootProject.layout.projectDirectory.dir("../rust-lang-kotlin").asFile
val verifiedRustUtf8CowFiles = setOf(
    "candidates/core/kotlin/org/rustlang/core/slice/SharedByteSlice.kt",
    "candidates/core/kotlin/org/rustlang/core/str/Utf8ByteOperations.kt",
    "candidates/alloc/kotlin/org/rustlang/alloc/borrow/Utf8Cow.kt",
).map { verifiedRustUtf8CowRoot.resolve(it) }.toSet()
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    exclude {
        !it.isDirectory && it.file.toPath().startsWith(verifiedRustUtf8CowRoot.toPath()) &&
            it.file !in verifiedRustUtf8CowFiles
    }
}
