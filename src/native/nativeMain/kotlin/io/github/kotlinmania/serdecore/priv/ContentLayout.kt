package io.github.kotlinmania.serdecore.priv

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.CpuArchitecture
import kotlin.native.OsFamily
import kotlin.native.Platform

@OptIn(ExperimentalNativeApi::class)
internal actual fun rustContentLayoutTarget(): String =
    if (Platform.osFamily == OsFamily.MACOSX && Platform.cpuArchitecture == CpuArchitecture.ARM64) {
        "aarch64-apple-darwin"
    } else {
        "${Platform.osFamily}/${Platform.cpuArchitecture}"
    }
