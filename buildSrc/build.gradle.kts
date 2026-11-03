plugins {
    java
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

// Only product-building helpers belong here. Standalone verification and workspace
// automation are compiled by tools/java-build-tool.sh, outside the product build.
dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

// Project-owned helpers use the parsers and archive libraries bundled with Gradle.
dependencies {
    implementation(fileTree(requireNotNull(gradle.gradleHomeDir).resolve("lib")) {
        include("tomlj-*.jar", "antlr4-runtime-*.jar", "commons-compress-*.jar", "commons-io-*.jar", "commons-codec-*.jar", "commons-lang3-*.jar")
    })
}
