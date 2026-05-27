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

plugins {
    kotlin("multiplatform") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"
    id("com.android.kotlin.multiplatform.library") version "9.2.1"
    id("com.vanniktech.maven.publish") version "0.36.0"
}

group = "io.github.kotlinmania"
version = "0.1.1"

// ============================================================================
// Android SDK installer
// ----------------------------------------------------------------------------
// The Android Gradle Plugin resolves the SDK location at configuration time,
// so the SDK must already be on disk before the `kotlin { android { ... } }`
// block evaluates. The installer is idempotent — a .install-complete marker
// short-circuits the download on every subsequent invocation, so warm runs
// pay only a directory-existence check. CI runners pay a one-time cold cost
// the first time they touch the project.
// ============================================================================

val androidCommandLineToolsRevision = "14742923"
val projectCompileSdk = "34"
val projectAndroidBuildTools = "36.0.0"
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

// Gate the (slow, network-bound) SDK download on whether any task in
// gradle.startParameter.taskNames looks like it needs Android. local.properties
// is always written so AGP's config-time sdk.dir resolution still succeeds.
// macOS / iOS / tvOS / watchOS / Linux / Windows / JS / Wasm / host-JVM
// invocations no longer pay the SDK-download tax. The `setupAndroidSdk` task
// remains the explicit entry point when the SDK has to be installed deliberately.
val androidTaskRequested = gradle.startParameter.taskNames.any { taskName ->
    val lower = taskName.lowercase()
    "android" in lower || "aar" in lower
}
val androidSdkExecOperations = serviceOf<ExecOperations>()
if (androidTaskRequested) {
    installProjectAndroidSdk(androidSdkExecOperations)
} else {
    writeAndroidLocalProperties()
    println(
        "setup-android-sdk: skipped install (no Android tasks in start parameter); " +
            "local.properties -> $projectAndroidSdkDir",
    )
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

    val xcf = XCFramework("Serde")

    // Apple
    macosArm64 { binaries.framework { baseName = "Serde"; xcf.add(this) } }
    iosArm64 { binaries.framework { baseName = "Serde"; isStatic = true; xcf.add(this) } }
    iosSimulatorArm64 { binaries.framework { baseName = "Serde"; isStatic = true; xcf.add(this) } }
    iosX64 { binaries.framework { baseName = "Serde"; isStatic = true; xcf.add(this) } }
    tvosArm64 { binaries.framework { baseName = "Serde"; xcf.add(this) } }
    tvosSimulatorArm64 { binaries.framework { baseName = "Serde"; xcf.add(this) } }
    watchosArm64 { binaries.framework { baseName = "Serde"; xcf.add(this) } }
    watchosDeviceArm64 { binaries.framework { baseName = "Serde"; xcf.add(this) } }
    watchosSimulatorArm64 { binaries.framework { baseName = "Serde"; xcf.add(this) } }

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
        moduleName = "Serde"
        flattenPackage = "io.github.kotlinmania.serde"
    }

    // Android KMP library
    android {
        namespace = "io.github.kotlinmania.serde"
        compileSdk = 34
        minSdk = 24
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
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
            implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.4.0")
            implementation("io.github.kotlinmania:proc-macro2-kotlin:0.1.2")
            implementation("io.github.kotlinmania:quote-kotlin:0.1.2")
            implementation("io.github.kotlinmania:syn-kotlin:0.1.6")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }

    jvmToolchain(21)
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
// JS / Wasm toolchain pins (Node + Yarn versions + yarn resolutions +
// vendored karma-webpack). Required because Kotlin/JS pulls a large npm
// dependency graph that has been the source of repeated CVE noise.
// ============================================================================
rootProject.extensions.configure<NodeJsEnvSpec>("kotlinNodeJsSpec") { version.set("24.15.0") }
rootProject.extensions.configure<WasmNodeJsEnvSpec>("kotlinWasmNodeJsSpec") { version.set("24.15.0") }
rootProject.extensions.configure<YarnRootEnvSpec>("kotlinYarnSpec") { version.set("1.22.22") }
rootProject.extensions.configure<WasmYarnRootEnvSpec>("kotlinWasmYarnSpec") { version.set("1.22.22") }
rootProject.extensions.configure<YarnRootExtension>("kotlinYarn") {
    resolution("diff", "8.0.3"); resolution("**/diff", "8.0.3")
    resolution("fast-uri", "3.1.2"); resolution("**/fast-uri", "3.1.2")
    resolution("serialize-javascript", "7.0.5"); resolution("**/serialize-javascript", "7.0.5")
    resolution("webpack", "5.106.2"); resolution("**/webpack", "5.106.2")
    resolution("follow-redirects", "1.16.0"); resolution("**/follow-redirects", "1.16.0")
    resolution("lodash", "4.18.1"); resolution("**/lodash", "4.18.1")
    resolution("ajv", "8.20.0"); resolution("**/ajv", "8.20.0")
    resolution("brace-expansion", "5.0.6"); resolution("**/brace-expansion", "5.0.6")
    resolution("flatted", "3.4.2"); resolution("**/flatted", "3.4.2")
    resolution("minimatch", "10.2.5"); resolution("**/minimatch", "10.2.5")
    resolution("picomatch", "4.0.4"); resolution("**/picomatch", "4.0.4")
    resolution("qs", "6.15.2"); resolution("**/qs", "6.15.2")
    resolution("socket.io-parser", "4.2.6"); resolution("**/socket.io-parser", "4.2.6")
    resolution("ws", "8.20.1"); resolution("**/ws", "8.20.1")
}
val patchedKarmaWebpackPackage = rootProject.layout.projectDirectory.dir("gradle/npm/karma-webpack").asFile.absolutePath.replace("\\", "/")
rootProject.extensions.configure<NodeJsRootExtension>("kotlinNodeJs") {
    versions.webpack.version = "5.106.2"
    versions.webpackCli.version = "7.0.2"
    versions.karma.version = "npm:karma-maintained@6.4.7"
    versions.karmaWebpack.version = "file:$patchedKarmaWebpackPackage"
    versions.mocha.version = "12.0.0-beta-10"
    versions.kotlinWebHelpers.version = "3.1.0"
}

// ============================================================================
// Maven Central publishing (vanniktech plugin)
// ============================================================================
mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), "serde-kotlin", version.toString())
    pom {
        name.set("serde-kotlin")
        description.set("Kotlin Multiplatform port of serde-rs/serde - A generic serialization/deserialization framework")
        inceptionYear.set("2026")
        url.set("https://github.com/KotlinMania/serde-kotlin")
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
            url.set("https://github.com/KotlinMania/serde-kotlin")
            connection.set("scm:git:git://github.com/KotlinMania/serde-kotlin.git")
            developerConnection.set("scm:git:ssh://github.com/KotlinMania/serde-kotlin.git")
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
    codeqlKotlinc("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.3.21")
    codeqlSourceClasspath("org.jetbrains.kotlin:kotlin-stdlib:2.3.21")
    codeqlSourceClasspath("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.11.0")
    codeqlSourceClasspath("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.11.0")
    codeqlSourceClasspath("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.11.0")
    codeqlSourceClasspath("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.8.0")
    codeqlSourceClasspath("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.4.0")
    codeqlAndroidAar("io.github.kotlinmania:proc-macro2-kotlin-android:0.1.2")
    codeqlAndroidAar("io.github.kotlinmania:quote-kotlin-android:0.1.2")
    codeqlAndroidAar("io.github.kotlinmania:syn-kotlin-android:0.1.6")
}
val codeqlCompileJvm = tasks.register<JavaExec>("codeqlCompileJvm") {
    description = "Compile commonMain Kotlin sources with kotlinc 2.3.21 for CodeQL Java/Kotlin extraction."
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
            "-language-version", "2.3",
            "-api-version", "2.3",
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
    description = "Downloads and configures the project-local Android SDK."
    doLast { installProjectAndroidSdk(androidSdkExecOperations) }
}

// The umbrella `test` task. Every entry is a REAL test task that runs ported
// Rust tests (no smoke / no compile-only placeholders). `findByName` lets the
// task degrade on hosts that can't run a particular platform's tests (e.g.
// macosArm64Test is null on a Linux runner).
tasks.register("test") {
    group = "verification"
    description = "Runs the host-portable real test suite (Rust ports only — no smoke tests)."
    val realTestTasks = listOf(
        "jvmTest",
        "macosArm64Test",
        "jsNodeTest",
        "wasmJsNodeTest",
        "wasmWasiNodeTest",
        "testAndroidHostTest",
    )
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
