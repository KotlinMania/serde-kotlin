import java.io.ByteArrayInputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream
import org.gradle.api.GradleException
import org.gradle.api.tasks.ClasspathNormalizer
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec

// Centralized versions from gradle.properties (used throughout the build script).
// NOTE: Plugin versions are supplied via gradle/libs.versions.toml version catalog.
val kotlinVersion = (project.findProperty("versions.kotlin") as? String) ?: "2.3.21"

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kmp)
    alias(libs.plugins.vanniktech)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

group = (project.findProperty("project.group") as? String) ?: "io.github.kotlinmania"
version = (project.findProperty("project.version") as? String) ?: "0.1.0-SNAPSHOT"
val frameworkName = (project.findProperty("project.frameworkName") as? String) ?: "Unnamed"
val projectNamespace = (project.findProperty("project.namespace") as? String) ?: "io.github.kotlinmania"

// ============================================================================
// Android SDK installer
// ----------------------------------------------------------------------------
// The Android Gradle Plugin resolves the SDK location at configuration time,
// so the SDK must already be on disk before the `kotlin { androidLibrary { ... } }`
// block evaluates. The installer is idempotent — a .install-complete marker
// short-circuits the download on every subsequent invocation, so warm runs
// pay only a directory-existence check. CI runners pay a one-time cold cost
// the first time they touch the project.
// ============================================================================

val androidCommandLineToolsRevision = (project.findProperty("android.commandLineTools.revision") as? String) ?: "14742923"
val projectCompileSdk = (project.findProperty("android.compileSdk") as? String) ?: "34"
val projectAndroidBuildTools = (project.findProperty("android.buildTools") as? String) ?: "36.0.0"
val isWindowsHost = System.getProperty("os.name").lowercase().contains("windows")
val isMacHost = System.getProperty("os.name").lowercase().contains("mac")
val androidSdkOsName = when {
    isWindowsHost -> "win"
    isMacHost -> "mac"
    System.getProperty("os.name").lowercase().contains("linux") -> "linux"
    else -> throw GradleException("Unsupported Android SDK setup OS: ${System.getProperty("os.name")}")
}
val projectAndroidSdkDir = layout.projectDirectory.dir(".android-sdk").asFile
val androidSdkManager = projectAndroidSdkDir.resolve(
    if (isWindowsHost) "cmdline-tools/latest/bin/sdkmanager.bat"
    else "cmdline-tools/latest/bin/sdkmanager",
)
val androidSdkInstallMarker = projectAndroidSdkDir.resolve(".install-complete")
val requiredAndroidSdkPackageDirs = listOf(
    projectAndroidSdkDir.resolve("platform-tools"),
    projectAndroidSdkDir.resolve("platforms/android-$projectCompileSdk"),
    projectAndroidSdkDir.resolve("build-tools/$projectAndroidBuildTools"),
)

fun writeAndroidLocalProperties() {
    val sdkDirPropertyValue = projectAndroidSdkDir.absolutePath.replace("\\", "/")
    layout.projectDirectory.file("local.properties").asFile.writeText("sdk.dir=$sdkDirPropertyValue\n")
}

fun isProjectAndroidSdkInstalled(): Boolean =
    androidSdkInstallMarker.exists() &&
        androidSdkManager.exists() &&
        requiredAndroidSdkPackageDirs.all { it.exists() }

fun sdkManagerCommand(vararg args: String): List<String> =
    if (isWindowsHost) listOf("cmd", "/c", androidSdkManager.absolutePath) + args
    else listOf(androidSdkManager.absolutePath) + args

fun downloadAndroidCommandLineTools() {
    val zipName = "commandlinetools-$androidSdkOsName-${androidCommandLineToolsRevision}_latest.zip"
    val url = "https://dl.google.com/android/repository/$zipName"
    val tmpDir = projectAndroidSdkDir.resolve(".tmp/commandline-tools")
    val zipFile = tmpDir.resolve(zipName)
    val latestDir = projectAndroidSdkDir.resolve("cmdline-tools/latest")
    println("setup-android-sdk: downloading $url")
    tmpDir.deleteRecursively()
    tmpDir.mkdirs()
    try {
        URI(url).toURL().openStream().use { input ->
            Files.copy(input, zipFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        latestDir.deleteRecursively()
        latestDir.mkdirs()
        val canonicalLatestDir = latestDir.canonicalFile.toPath()
        ZipInputStream(zipFile.inputStream().buffered()).use { zipInput ->
            generateSequence { zipInput.nextEntry }.forEach { entry ->
                val relativeName = entry.name.removePrefix("cmdline-tools/").trimStart('/')
                if (relativeName.isNotEmpty()) {
                    val target = latestDir.resolve(relativeName).canonicalFile
                    if (!target.toPath().startsWith(canonicalLatestDir)) {
                        throw GradleException("Refusing to extract Android SDK entry outside $latestDir: ${entry.name}")
                    }
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile.mkdirs()
                        Files.copy(zipInput, target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        if (!isWindowsHost && relativeName.startsWith("bin/")) target.setExecutable(true)
                    }
                }
                zipInput.closeEntry()
            }
        }
        if (!isWindowsHost) androidSdkManager.setExecutable(true)
    } finally {
        tmpDir.deleteRecursively()
    }
}

fun installProjectAndroidSdk(execOperations: ExecOperations) {
    if (isProjectAndroidSdkInstalled()) {
        writeAndroidLocalProperties()
        println("setup-android-sdk: SDK already installed at $projectAndroidSdkDir")
        return
    }
    if (!androidSdkManager.exists()) downloadAndroidCommandLineTools()
    println("setup-android-sdk: accepting licenses")
    val licenseAnswers = "y\n".repeat(200).toByteArray(Charsets.UTF_8)
    val licenseResult = execOperations.exec {
        commandLine(sdkManagerCommand("--sdk_root=${projectAndroidSdkDir.absolutePath}", "--licenses"))
        standardInput = ByteArrayInputStream(licenseAnswers)
        isIgnoreExitValue = true
    }
    if (licenseResult.exitValue != 0) {
        throw GradleException("Android SDK license acceptance failed with exit code ${licenseResult.exitValue}")
    }
    println("setup-android-sdk: installing platform-tools, android-$projectCompileSdk, build-tools;$projectAndroidBuildTools")
    val installLog = projectAndroidSdkDir.resolve("sdkmanager-install.log")
    installLog.parentFile.mkdirs()
    installLog.outputStream().use { output ->
        val installResult = execOperations.exec {
            commandLine(sdkManagerCommand(
                "--sdk_root=${projectAndroidSdkDir.absolutePath}",
                "platform-tools",
                "platforms;android-$projectCompileSdk",
                "build-tools;$projectAndroidBuildTools",
            ))
            standardOutput = output
            errorOutput = output
            isIgnoreExitValue = true
        }
        if (installResult.exitValue != 0) {
            throw GradleException(
                "Android SDK package install failed with exit code ${installResult.exitValue}. " +
                    "Install log:\n${installLog.readText()}",
            )
        }
    }
    writeAndroidLocalProperties()
    androidSdkInstallMarker.writeText("")
    println("setup-android-sdk: done; SDK at $projectAndroidSdkDir")
}

// New policy: do NOT guess from start tasks. Always wire local.properties at
// configuration time so AGP can resolve sdk.dir, and make a REAL Android task
// responsible for ensuring the SDK is installed on first use.
//
// We keep the installer purely programmatic (no bash): downloads the
// cmdline-tools ZIP, accepts licenses, installs exact packages, writes a
// marker, and short-circuits on subsequent runs.

// Always ensure local.properties exists for AGP configuration time.
writeAndroidLocalProperties()

// Idempotent task that installs the SDK only when missing. This runs inside
// the Gradle build (no external shell). It can be depended on by any Android
// task that truly needs the SDK; by default we hook it to `compileAndroidMain`.
val ensureAndroidSdk by tasks.registering {
    group = "setup"
    description = "Ensures the project-local Android SDK is installed (idempotent)."
    // Skip the action when everything we need is already in place.
    onlyIf { !isProjectAndroidSdkInstalled() }
    doLast {
        installProjectAndroidSdk(serviceOf())
    }
}

// Make the primary Android compilation task responsible for ensuring the SDK
// is present. Other Android tasks typically depend on this one transitively.
// If the task does not exist on this host, the configuration is a no-op.
tasks.matching { it.name == "compileAndroidMain" }.configureEach {
    dependsOn(ensureAndroidSdk)
}

// ============================================================================
// kotlin { … } — every target Kotlin supports, no per-target source-set forks
// ============================================================================
// applyDefaultHierarchyTemplate() wires every intermediate source set the
// default hierarchy provides (commonMain → native → apple → {ios, macos,
// tvos, watchos}; commonMain → web → {js, wasmJs}; commonMain → wasmWasi;
// commonMain → jvm; commonMain → android; commonMain → native → linux /
// mingw / androidNative). Code stays in commonMain unless a hardware actual
// truly requires a leaf. Retired targets (watchosArm32, tvosX64, watchosX64,
// macosX64) stay retired per workspace template policy.
kotlin {
    applyDefaultHierarchyTemplate()

    sourceSets.all {
        languageSettings.optIn("kotlin.time.ExperimentalTime")
        languageSettings.optIn("kotlin.concurrent.atomics.ExperimentalAtomicApi")
    }

    compilerOptions {
        allWarningsAsErrors.set(true)
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    val xcf = XCFramework(frameworkName)

    // Apple
    macosArm64 { binaries.framework { baseName = frameworkName; xcf.add(this) } }
    iosArm64 { binaries.framework { baseName = frameworkName; isStatic = true; xcf.add(this) } }
    iosSimulatorArm64 { binaries.framework { baseName = frameworkName; isStatic = true; xcf.add(this) } }
    iosX64 { binaries.framework { baseName = frameworkName; isStatic = true; xcf.add(this) } }
    tvosArm64 { binaries.framework { baseName = frameworkName; xcf.add(this) } }
    tvosSimulatorArm64 { binaries.framework { baseName = frameworkName; xcf.add(this) } }
    watchosArm64 { binaries.framework { baseName = frameworkName; xcf.add(this) } }
    watchosDeviceArm64 { binaries.framework { baseName = frameworkName; xcf.add(this) } }
    watchosSimulatorArm64 { binaries.framework { baseName = frameworkName; xcf.add(this) } }

    // Other native
    linuxX64()
    linuxArm64()
    mingwX64()

    // Android NDK
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()

    // Web
    js { browser(); nodejs() }
    @OptIn(ExperimentalWasmDsl::class) wasmJs { browser(); nodejs() }
    @OptIn(ExperimentalWasmDsl::class) wasmWasi { nodejs() }

    // Swift Export bridge
    swiftExport {
        moduleName = frameworkName
        flattenPackage = projectNamespace
    }

    // Android KMP library
    android {
        namespace = projectNamespace
        compileSdk = ((project.findProperty("android.compileSdk") as? String) ?: "34").toInt()
        minSdk = ((project.findProperty("android.minSdk") as? String) ?: "24").toInt()
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder { sourceSetTreeName = "test" }
    }

    // JVM
    jvm()

    // Dependencies — multiplatform libraries declared once in commonMain.
    // The Kotlin Gradle plugin auto-pulls per-platform variants into every
    // other source set, including jvmMain and androidMain. No per-leaf
    // overrides because serde-kotlin's logic is pure Kotlin with zero FFI.
    sourceSets {
        commonMain.dependencies {
            // Use version-catalog aliases instead of stringly-typed properties.
            implementation(libs.bundles.serde.commonMain)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }

    jvmToolchain(((project.findProperty("jvm.toolchain") as? String) ?: "21").toInt())
}

// ============================================================================
// Test logging — full stack traces + standard streams on every test task.
// ============================================================================
tasks.withType<AbstractTestTask>().configureEach {
    testLogging {
        events(
            TestLogEvent.STARTED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
            TestLogEvent.FAILED,
            TestLogEvent.STANDARD_OUT,
            TestLogEvent.STANDARD_ERROR,
        )
        exceptionFormat = TestExceptionFormat.FULL
        showCauses = true
        showExceptions = true
        showStackTraces = true
        showStandardStreams = true
    }
}

// ============================================================================
// Static analysis: Detekt (code smells, potential-bugs) + Ktlint (format)
// =========================================================================
detekt {
    buildUponDefaultConfig = true
    allRules = false
    autoCorrect = false
    source.setFrom(files("src"))
    config.setFrom(files("detekt.yml"))
    parallel = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        sarif.required.set(true)
        txt.required.set(false)
        xml.required.set(false)
    }
}

ktlint {
    debug.set(false)
    verbose.set(false)
    android.set(false)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.SARIF)
    }
    filter {
        exclude("**/build/**")
        include("**/src/**/kotlin/**")
    }
}

// Make `check` run static analysis. Build gate already pulls `check` via tests and others.
tasks.matching { it.name == "check" }.configureEach {
    dependsOn("detekt")
    dependsOn("ktlintCheck")
}

// ============================================================================
// JS / Wasm toolchain pins (Node + Yarn versions + yarn resolutions +
// vendored karma-webpack). Required because Kotlin/JS pulls a large npm
// dependency graph that has been the source of repeated CVE noise.
// ============================================================================
val nodeVersion = (project.findProperty("node.version") as? String) ?: "24.15.0"
val wasmNodeVersion = (project.findProperty("wasm.node.version") as? String) ?: nodeVersion
val yarnVersion = (project.findProperty("yarn.version") as? String) ?: "1.22.22"
val wasmYarnVersion = (project.findProperty("wasm.yarn.version") as? String) ?: yarnVersion

rootProject.extensions.configure<NodeJsEnvSpec>("kotlinNodeJsSpec") { version.set(nodeVersion) }
rootProject.extensions.configure<WasmNodeJsEnvSpec>("kotlinWasmNodeJsSpec") { version.set(wasmNodeVersion) }
rootProject.extensions.configure<YarnRootEnvSpec>("kotlinYarnSpec") { version.set(yarnVersion) }
rootProject.extensions.configure<WasmYarnRootEnvSpec>("kotlinWasmYarnSpec") { version.set(wasmYarnVersion) }
rootProject.extensions.configure<YarnRootExtension>("kotlinYarn") {
    fun prop(name: String, default: String) = (project.findProperty(name) as? String) ?: default
    resolution("diff", prop("yarn.resolution.diff", "8.0.3")); resolution("**/diff", prop("yarn.resolution.diff", "8.0.3"))
    resolution("fast-uri", prop("yarn.resolution.fast-uri", "3.1.2")); resolution("**/fast-uri", prop("yarn.resolution.fast-uri", "3.1.2"))
    resolution("serialize-javascript", prop("yarn.resolution.serialize-javascript", "7.0.5")); resolution("**/serialize-javascript", prop("yarn.resolution.serialize-javascript", "7.0.5"))
    resolution("webpack", prop("yarn.resolution.webpack", "5.106.2")); resolution("**/webpack", prop("yarn.resolution.webpack", "5.106.2"))
    resolution("follow-redirects", prop("yarn.resolution.follow-redirects", "1.16.0")); resolution("**/follow-redirects", prop("yarn.resolution.follow-redirects", "1.16.0"))
    resolution("lodash", prop("yarn.resolution.lodash", "4.18.1")); resolution("**/lodash", prop("yarn.resolution.lodash", "4.18.1"))
    resolution("ajv", prop("yarn.resolution.ajv", "8.20.0")); resolution("**/ajv", prop("yarn.resolution.ajv", "8.20.0"))
    resolution("brace-expansion", prop("yarn.resolution.brace-expansion", "5.0.6")); resolution("**/brace-expansion", prop("yarn.resolution.brace-expansion", "5.0.6"))
    resolution("flatted", prop("yarn.resolution.flatted", "3.4.2")); resolution("**/flatted", prop("yarn.resolution.flatted", "3.4.2"))
    resolution("minimatch", prop("yarn.resolution.minimatch", "10.2.5")); resolution("**/minimatch", prop("yarn.resolution.minimatch", "10.2.5"))
    resolution("picomatch", prop("yarn.resolution.picomatch", "4.0.4")); resolution("**/picomatch", prop("yarn.resolution.picomatch", "4.0.4"))
    resolution("qs", prop("yarn.resolution.qs", "6.15.2")); resolution("**/qs", prop("yarn.resolution.qs", "6.15.2"))
    resolution("socket.io-parser", prop("yarn.resolution.socket-io-parser", "4.2.6")); resolution("**/socket.io-parser", prop("yarn.resolution.socket-io-parser", "4.2.6"))
    resolution("ws", prop("yarn.resolution.ws", "8.20.1")); resolution("**/ws", prop("yarn.resolution.ws", "8.20.1"))
    resolution("tmp", prop("yarn.resolution.tmp", "0.2.6")); resolution("**/tmp", prop("yarn.resolution.tmp", "0.2.6"))
}
val patchedKarmaWebpackPackage = rootProject.layout.projectDirectory.dir("gradle/npm/karma-webpack").asFile.absolutePath.replace("\\", "/")
rootProject.extensions.configure<NodeJsRootExtension>("kotlinNodeJs") {
    versions.webpack.version = (project.findProperty("node.webpack.version") as? String) ?: "5.106.2"
    versions.webpackCli.version = (project.findProperty("node.webpackCli.version") as? String) ?: "7.0.2"
    versions.karma.version = (project.findProperty("node.karma.version") as? String) ?: "npm:karma-maintained@6.4.7"
    versions.karmaWebpack.version = "file:$patchedKarmaWebpackPackage"
    versions.mocha.version = (project.findProperty("node.mocha.version") as? String) ?: "12.0.0-beta-10"
    versions.kotlinWebHelpers.version = (project.findProperty("node.kotlinWebHelpers.version") as? String) ?: "3.1.0"
}

// ============================================================================
// Maven Central publishing (vanniktech plugin)
// ============================================================================
mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    val projectName = (project.findProperty("project.name") as? String) ?: "unnamed-project"
    coordinates(group.toString(), projectName, version.toString())
    pom {
        name.set(projectName)
        description.set((project.findProperty("project.pom.description") as? String) ?: "")
        inceptionYear.set("2026")
        url.set("https://github.com/KotlinMania/$projectName")
        licenses {
            license {
                name.set("MIT")
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
            url.set("https://github.com/KotlinMania/$projectName")
            connection.set("scm:git:git://github.com/KotlinMania/$projectName.git")
            developerConnection.set("scm:git:ssh://github.com/KotlinMania/$projectName.git")
        }
    }
}

// ============================================================================
// CodeQL extraction — commonMain compiled as plain JVM kotlinc against
// kotlinmania siblings' published Android AARs so the Java/Kotlin analyzer
// can hook the compiler directly.
// ============================================================================
val codeqlKotlinc: Configuration by configurations.creating {
    description = "Kotlin compiler (CodeQL extraction target only - not published)"
    isCanBeResolved = true
    isCanBeConsumed = false
}
val codeqlSourceClasspath: Configuration by configurations.creating {
    description = "Runtime classpath for CodeQL extraction of commonMain sources"
    isCanBeResolved = true
    isCanBeConsumed = false
}
val codeqlAndroidAar: Configuration by configurations.creating {
    description = "Android AAR artifacts for CodeQL dependency classpath extraction (classes.jar only)"
    isCanBeResolved = true
    isCanBeConsumed = false
}
dependencies {
    val codeqlKotlinVersion = (project.findProperty("codeql.kotlin.version") as? String) ?: kotlinVersion
    codeqlKotlinc("org.jetbrains.kotlin:kotlin-compiler-embeddable:$codeqlKotlinVersion")
    
    val codeqlSourceDeps = (project.findProperty("project.dependencies.codeqlSourceClasspath") as? String) ?: ""
    codeqlSourceDeps.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { dep ->
        codeqlSourceClasspath(dep)
    }

    val codeqlAars = (project.findProperty("project.dependencies.codeqlAndroidAar") as? String) ?: ""
    codeqlAars.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { aar ->
        codeqlAndroidAar(aar)
    }
}
val codeqlLanguageVersion = (project.findProperty("kotlin.languageVersion") as? String) ?: kotlinVersion.substringBeforeLast('.')
val codeqlApiVersion = (project.findProperty("kotlin.apiVersion") as? String) ?: codeqlLanguageVersion

val codeqlCompileJvm = tasks.register<JavaExec>("codeqlCompileJvm") {
    description = "Compile commonMain Kotlin sources with kotlinc $codeqlLanguageVersion for CodeQL Java/Kotlin extraction."
    group = "verification"
    classpath(codeqlKotlinc)
    mainClass.set("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
    val outDir = layout.buildDirectory.dir("classes/kotlin/codeql-jvm")
    val aarExtractDir = layout.buildDirectory.dir("codeql/android-aar")
    val sources = fileTree("src/commonMain/kotlin") { include("**/*.kt") }
    val sentinelDir = layout.buildDirectory.dir("generated/codeql-empty-source")
    inputs.files(sources).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(codeqlSourceClasspath).withNormalizer(ClasspathNormalizer::class.java)
    inputs.files(codeqlAndroidAar).withNormalizer(ClasspathNormalizer::class.java)
    outputs.dir(outDir)
    outputs.dir(aarExtractDir)
    outputs.dir(sentinelDir)
    doFirst {
        outDir.get().asFile.mkdirs()
        val extractedJars = mutableListOf<File>()
        for (aar in codeqlAndroidAar.resolve()) {
            val extractTarget = aarExtractDir.get().asFile.resolve(aar.nameWithoutExtension)
            extractTarget.mkdirs()
            copy {
                from(zipTree(aar))
                include("classes.jar")
                into(extractTarget)
            }
            val classesJar = extractTarget.resolve("classes.jar")
            if (classesJar.exists()) extractedJars += classesJar
        }
        val fullClasspath =
            (codeqlSourceClasspath.resolve() + extractedJars)
                .joinToString(File.pathSeparator) { it.absolutePath }
        val sourceFiles = sources.files.toMutableList()
        if (sourceFiles.isEmpty()) {
            val sentinelFile = sentinelDir.get().asFile.resolve("io/github/kotlinmania/codeql/_CodeqlEmptySource.kt")
            sentinelFile.parentFile.mkdirs()
            sentinelFile.writeText(
                """
                package io.github.kotlinmania.codeql

                private object _CodeqlEmptySource
                """.trimIndent(),
            )
            sourceFiles += sentinelFile
        }
        args = listOf(
            "-d", outDir.get().asFile.absolutePath,
            "-classpath", fullClasspath,
            "-jvm-target", "21",
            "-no-stdlib",
            "-no-reflect",
            "-language-version", codeqlLanguageVersion,
            "-api-version", codeqlApiVersion,
            "-Xexpect-actual-classes",
            "-opt-in", "kotlin.time.ExperimentalTime",
            "-opt-in", "kotlin.concurrent.atomics.ExperimentalAtomicApi",
        ) + sourceFiles.map { it.absolutePath }
    }
}

// ============================================================================
// Tasks
// ============================================================================

tasks.register("setupAndroidSdk") {
    group = "setup"
    description = "Downloads and configures the project-local Android SDK. (Alias for ensureAndroidSdk)"
    dependsOn("ensureAndroidSdk")
}

// The umbrella `test` task. Every entry is a REAL test task that runs ported
// Rust tests (no smoke / no compile-only placeholders). `findByName` lets the
// task degrade on hosts that can't run a particular platform's tests (e.g.
// macosArm64Test is null on a Linux runner).
tasks.register("test") {
    group = "verification"
    description = "Runs the host-portable real test suite (Rust ports only — no smoke tests)."
    val realTestTasks = mutableListOf(
        "jvmTest",
        "macosArm64Test",
        "jsNodeTest",
        "wasmJsNodeTest",
        "wasmWasiNodeTest"
    )
    if (isProjectAndroidSdkInstalled()) {
        realTestTasks.add("testAndroidHostTest")
    }
    dependsOn(realTestTasks.mapNotNull { tasks.findByName(it) })
}

// Skip the Xcode-embed task unless Xcode has set the environment that
// embedSwiftExportForXcode needs, OR the task was explicitly requested.
val xcodeSwiftExportEnvironmentNames = listOf(
    "SDK_NAME",
    "CONFIGURATION",
    "TARGET_BUILD_DIR",
    "BUILT_PRODUCTS_DIR",
    "ARCHS",
    "FRAMEWORKS_FOLDER_PATH",
    "DEPLOYMENT_TARGET_SETTING_NAME",
)
fun hasXcodeSwiftExportEnvironment(): Boolean {
    if (!xcodeSwiftExportEnvironmentNames.all { !System.getenv(it).isNullOrBlank() }) return false
    val deploymentTargetSettingName = System.getenv("DEPLOYMENT_TARGET_SETTING_NAME")
    return !System.getenv(deploymentTargetSettingName).isNullOrBlank()
}
val swiftExportTaskDirectlyRequested =
    gradle.startParameter.taskNames.any { it == "embedSwiftExportForXcode" || it.endsWith(":embedSwiftExportForXcode") }
tasks.matching { it.name == "embedSwiftExportForXcode" }.configureEach {
    onlyIf {
        val hasXcodeEnvironment = hasXcodeSwiftExportEnvironment()
        if (!hasXcodeEnvironment && !swiftExportTaskDirectlyRequested) {
            logger.lifecycle("embedSwiftExportForXcode: skipped because Xcode environment variables are not present")
        }
        hasXcodeEnvironment || swiftExportTaskDirectlyRequested
    }
}

// ============================================================================
// `build` aggregate
// ----------------------------------------------------------------------------
// The Kotlin Multiplatform plugin registers per-target tasks (*MainClasses,
// *TestClasses, *Binaries, *XCFramework, embedSwiftExportForXcode, the
// metadata-export tasks) AFTER `kotlin { ... }` finishes evaluating. The
// afterEvaluate matcher below picks every one of them up at once, so we
// don't maintain a hand-curated 100+ line task list that goes stale when
// the plugin adds new tasks.
// ============================================================================
val fullTargetBuildTaskNames = setOf(
    // Android KMP
    "compileAndroidMain", "compileAndroidHostTest", "compileAndroidDeviceTest",
    "assembleAndroidMain", "assembleUnitTest", "assembleAndroidTest",
    "assembleAndroidDeviceTest", "testAndroidHostTest",
    // JVM
    "jvmMainClasses", "jvmTestClasses",
    // JS / Wasm
    "jsMainClasses", "jsTestClasses",
    "wasmJsMainClasses", "wasmJsTestClasses",
    "wasmWasiMainClasses", "wasmWasiTestClasses",
    // Native binaries + test binaries
    "androidNativeArm32Binaries",    "androidNativeArm32TestBinaries",
    "androidNativeArm64Binaries",    "androidNativeArm64TestBinaries",
    "androidNativeX64Binaries",      "androidNativeX64TestBinaries",
    "androidNativeX86Binaries",      "androidNativeX86TestBinaries",
    "iosArm64Binaries",              "iosArm64TestBinaries",
    "iosSimulatorArm64Binaries",     "iosSimulatorArm64TestBinaries",
    "iosX64Binaries",                "iosX64TestBinaries",
    "linuxArm64Binaries",            "linuxArm64TestBinaries",
    "linuxX64Binaries",              "linuxX64TestBinaries",
    "macosArm64Binaries",            "macosArm64TestBinaries",
    "mingwX64Binaries",              "mingwX64TestBinaries",
    "tvosArm64Binaries",             "tvosArm64TestBinaries",
    "tvosSimulatorArm64Binaries",    "tvosSimulatorArm64TestBinaries",
    "watchosArm64Binaries",          "watchosArm64TestBinaries",
    "watchosDeviceArm64Binaries",    "watchosDeviceArm64TestBinaries",
    "watchosSimulatorArm64Binaries", "watchosSimulatorArm64TestBinaries",
    // Swift Export + XCFramework
    "embedSwiftExportForXcode",
    "assemble${frameworkName}XCFramework",
)

tasks.named("build") { dependsOn(fullTargetBuildTaskNames) }

afterEvaluate {
    tasks.named("build") {
        dependsOn(
            tasks.matching {
                name.endsWith("MainClasses") ||
                    name.endsWith("TestClasses") ||
                    name.endsWith("Binaries") ||
                    name.endsWith("XCFramework") ||
                    name == "embedSwiftExportForXcode" ||
                    name.startsWith("exportCommonSourceSetsMetadataLocationsFor") ||
                    name.startsWith("exportRootPublicationCoordinatesFor") ||
                    name.startsWith("exportCrossCompilationMetadataFor") ||
                    name.startsWith("exportTargetPublicationCoordinatesFor")
            },
        )
    }
}
