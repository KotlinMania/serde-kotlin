/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import ru.vyarus.gradle.plugin.animalsniffer.AnimalSnifferExtension

pluginManager.withPlugin("org.gradle.java-base") {
    apply(plugin = "ru.vyarus.animalsniffer")

    configure<AnimalSnifferExtension> {
        defaultTargets = setOf("jvmMain")
    }
    val signature = configurations.getByName("signature")
    dependencies {
        // Use the same API level as OkHttp
        add(signature.name, "net.sf.androidscents.signature:android-api-level-21:5.0.1_r2@signature")
    }
}
