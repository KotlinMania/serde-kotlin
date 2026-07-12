plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral()
    if (project.hasSigningKey()) {
        signAllPublications()
    }

    coordinates(project.group.toString(), project.name, project.version.toString())

    pom {
        name.set(project.name)
        description.set("Kotlin Multiplatform port of the Rust ${project.name} crate")
        inceptionYear.set("2026")
        url.set("https://github.com/KotlinMania/serde-kotlin")

        licenses {
            license {
                name.set("MIT OR Apache-2.0")
                url.set("https://opensource.org/licenses/MIT")
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
