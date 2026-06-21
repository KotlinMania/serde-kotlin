// serde_core — the serde_core analogue: trait/data-model definitions, no derive.
// Slimmed to the shared convention plugins (build-logic); module-specific config only.
// Publishing is intentionally NOT applied here yet — vanniktech is being replaced.

plugins {
    id("kotlinx-io-multiplatform")
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
