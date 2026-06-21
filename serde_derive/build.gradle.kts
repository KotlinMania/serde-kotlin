// serde_derive — the serde_derive analogue: the derive/codegen port.
// Depends on :serde-core and the published syn/quote/proc-macro2 ports (nothing vendored).
// Slimmed to the shared convention plugins (build-logic). Publishing not applied yet —
// vanniktech is being replaced.

plugins {
    id("kotlinx-io-multiplatform")
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
