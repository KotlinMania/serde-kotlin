// serde_derive — the serde_derive analogue: the derive/codegen port.
// Depends on :serde-core and the published syn/quote/proc-macro2 ports (nothing vendored).
// This module publishes separately as io.github.kotlinmania:serde-derive.

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
        namespace = "io.github.kotlinmania.serderive"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":serde-core"))
            implementation(libs.bundles.serde.derive.commonMain)
        }
    }
}
