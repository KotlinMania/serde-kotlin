/*
 * Publishing convention — Gradle-native maven-publish + signing.
 *
 * Replaces com.vanniktech.maven.publish. The vanniktech plugin owns the
 * publication model, which makes attaching arbitrary artifacts (C++ N-API
 * .node/.so, cpp prebuilds) impractical. Native maven-publish exposes the raw
 * publications { } / artifact(...) surface, so extra native artifacts can be
 * attached to the Maven coordinates by a module's own build script.
 *
 * The Kotlin Multiplatform plugin already registers one MavenPublication per
 * target plus the root `kotlinMultiplatform` publication and emits the sources
 * JARs; this convention adds the Central-required Javadoc JAR, the POM, signing,
 * and the publishing repository.
 */

import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.jvm.tasks.Jar
import org.gradle.plugins.signing.Sign

plugins {
    id("maven-publish")
    id("signing")
}

// Maven Central requires a Javadoc JAR on every publication. Real API docs are
// produced by the dokka convention; this is the required (empty) stub.
val javadocJar =
    tasks.register<Jar>("javadocJar") {
        archiveClassifier.set("javadoc")
    }

publishing {
    publications.withType<MavenPublication>().configureEach {
        artifact(javadocJar)
        pom {
            name.set(artifactId)
            description.set("Kotlin Multiplatform port of the Rust serde crate")
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

    repositories {
        maven {
            name = "MavenCentral"
            val isSnapshot = version.toString().endsWith("SNAPSHOT")
            val releaseUrl =
                providers.gradleProperty("publish.releaseRepositoryUrl")
                    .getOrElse("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            val snapshotUrl =
                providers.gradleProperty("publish.snapshotRepositoryUrl")
                    .getOrElse("https://central.sonatype.com/repository/maven-snapshots/")
            url = uri(if (isSnapshot) snapshotUrl else releaseUrl)
            credentials {
                username =
                    providers.gradleProperty("mavenCentralUsername")
                        .orElse(providers.environmentVariable("ORG_GRADLE_PROJECT_mavenCentralUsername")).orNull
                password =
                    providers.gradleProperty("mavenCentralPassword")
                        .orElse(providers.environmentVariable("ORG_GRADLE_PROJECT_mavenCentralPassword")).orNull
            }
        }
    }
}

signing {
    val signingKey =
        providers.gradleProperty("signingInMemoryKey")
            .orElse(providers.environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKey")).orNull
    val signingPassword =
        providers.gradleProperty("signingInMemoryKeyPassword")
            .orElse(providers.environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKeyPassword")).orNull
    if (!signingKey.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword ?: "")
        sign(publishing.publications)
    }
}

// Gradle requires an explicit dependency from publish tasks to the signing tasks
// (KMP + cross-publication signing otherwise trips the implicit-dependency check).
tasks.withType<AbstractPublishToMaven>().configureEach {
    mustRunAfter(tasks.withType<Sign>())
}
