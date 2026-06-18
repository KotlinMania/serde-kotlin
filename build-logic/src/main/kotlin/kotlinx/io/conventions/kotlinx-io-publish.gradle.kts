/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

plugins {
    id("com.vanniktech.maven.publish")
}

val publishedArtifactId =
    if (project.name == "serde-kotlin-core") {
        "serde-kotlin"
    } else {
        project.name
    }

mavenPublishing {
    publishToMavenCentral()
    if (project.hasSigningKey()) {
        signAllPublications()
    }

    coordinates(project.group.toString(), publishedArtifactId, project.version.toString())

    pom {
        name.set(publishedArtifactId)
        description.set("Kotlin multiplatform port of the Rust serde crate")
        inceptionYear.set("2026")
        url.set("https://github.com/KotlinMania/serde-kotlin")

        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("sydneyrenee")
                name.set("Sydney Renee")
                email.set("sydney@solace.ofharmony.ai")
                url.set("https://github.com/sydneyrenee")
            }
        }

        scm {
            url.set("https://github.com/KotlinMania/serde-kotlin")
            connection.set("scm:git:git://github.com/KotlinMania/serde-kotlin.git")
            developerConnection.set("scm:git:ssh://github.com/KotlinMania/serde-kotlin.git")
        }
    }
}

fun Project.hasSigningKey(): Boolean =
    !(findProperty("signingInMemoryKey") as? String).isNullOrBlank() ||
        !System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey").isNullOrBlank()
