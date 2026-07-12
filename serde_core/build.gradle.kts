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
    android {
        namespace = "io.github.kotlinmania.serdecore"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.bundles.serde.commonMain)
        }
    }
}
