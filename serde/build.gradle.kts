import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.process.ExecOperations

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
        namespace = "io.github.kotlinmania.serde"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":serde-core"))
            api(project(":serde-derive"))
        }
    }
}

val fullTargetBuildTasks = listOf(
    "compileAndroidMain",
    "compileAndroidHostTest",
    "compileAndroidDeviceTest",
    "assembleAndroidMain",
    "assembleUnitTest",
    "assembleAndroidTest",
    "assembleAndroidDeviceTest",
    "testAndroidHostTest",
    "jvmMainClasses",
    "jvmTestClasses",
    "jvmTest",
    "jsMainClasses",
    "jsTestClasses",
    "jsBrowserTest",
    "jsNodeTest",
    "jsTest",
    "wasmJsMainClasses",
    "wasmJsTestClasses",
    "wasmJsBrowserTest",
    "wasmJsNodeTest",
    "wasmJsTest",
    "wasmWasiMainClasses",
    "wasmWasiTestClasses",
    "wasmWasiNodeTest",
    "wasmWasiTest",
    "androidNativeArm64Binaries",
    "androidNativeArm64TestBinaries",
    "androidNativeX64Binaries",
    "androidNativeX64TestBinaries",
    "iosArm64Binaries",
    "iosArm64TestBinaries",
    "iosSimulatorArm64Binaries",
    "iosSimulatorArm64TestBinaries",
    "iosX64Binaries",
    "iosX64TestBinaries",
    "linuxArm64Binaries",
    "linuxArm64TestBinaries",
    "linuxX64Binaries",
    "linuxX64TestBinaries",
    "macosArm64Binaries",
    "macosArm64TestBinaries",
    "mingwX64Binaries",
    "mingwX64TestBinaries",
    "tvosArm64Binaries",
    "tvosArm64TestBinaries",
    "tvosSimulatorArm64Binaries",
    "tvosSimulatorArm64TestBinaries",
    "watchosArm64Binaries",
    "watchosArm64TestBinaries",
    "watchosDeviceArm64Binaries",
    "watchosDeviceArm64TestBinaries",
    "watchosSimulatorArm64Binaries",
    "watchosSimulatorArm64TestBinaries",
)

tasks.named("build") {
    dependsOn(fullTargetBuildTasks)
}

// ============================================================================
// Swift Export Configuration
// ============================================================================

kotlin {
    swiftExport {
        @OptIn(org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl::class)
        moduleName = "Serde"
    }
}

fun patchSwiftPackage(packageDir: File) {
    if (!packageDir.exists()) return
    val swiftExportArchiveName = "libSerde.a"

    packageDir
        .walkTopDown()
        .filter { it.isFile && it.name == "Package.swift" }
        .forEach { packageSwift ->
            val text = packageSwift.readText()
            var patched =
                text
                    .replace("platforms: [.macOS(.v13), .iOS(.v13)],", "platforms: [.macOS(.v14), .iOS(.v13)],")
                    .replace(
                        "            publicHeadersPath: \"include\"",
                        "            publicHeadersPath: \"include\",\n" +
                            "            cSettings: [\n" +
                            "                .unsafeFlags([\"-fno-objc-arc\"])\n" +
                            "            ]",
                    )
            if (!patched.contains("platforms:")) {
                patched =
                    patched.replaceFirst(
                        Regex("(name:\\s*\"[^\"]*\",)"),
                        "\$1\n    platforms: [.macOS(.v14)],",
                    )
            }
            if (patched != text) {
                packageSwift.writeText(patched)
            }
        }
    packageDir
        .walkTopDown()
        .filter { it.isFile && it.extension == "swift" }
        .forEach { swiftFile ->
            val text = swiftFile.readText()
            val patched =
                text
                    .replace("Foundation.NSInputStream", "Foundation.InputStream")
                    .replace("Foundation.NSOutputStream", "Foundation.OutputStream")
            if (patched != text) {
                swiftFile.writeText(patched)
            }
        }
    packageDir
        .walkTopDown()
        .filter { it.isFile && it.name == "module.modulemap" }
        .forEach { moduleMap ->
            val text = moduleMap.readText()
            val patched =
                Regex("""link "[^"]+"""").replace(text) {
                    "link \"$swiftExportArchiveName\""
                }
            if (patched != text) {
                moduleMap.writeText(patched)
            }
        }
}

val patchMacosArm64DebugSwiftPackage =
    tasks.register("patchMacosArm64DebugSwiftPackage") {
        dependsOn("macosArm64DebugGenerateSPMPackage")
        doLast {
            patchSwiftPackage(
                layout.buildDirectory
                    .dir("SPMPackage/macosArm64/Debug")
                    .get()
                    .asFile,
            )
        }
    }

tasks.configureEach {
    if (name == "macosArm64DebugBuildSPMPackage") {
        dependsOn(patchMacosArm64DebugSwiftPackage)
    }
}

tasks.register("swiftExportSmokeTest") {
    group = "verification"
    description = "Builds the Swift Export SPM package and runs swift test against it."
    outputs.upToDateWhen { false }

    doLast {
        val execOperations = serviceOf<org.gradle.process.ExecOperations>()
        val swiftBuildDir =
            layout.buildDirectory
                .dir("swift-test")
                .get()
                .asFile
                .absolutePath
        execOperations
            .exec {
                workingDir = projectDir
                commandLine(
                    "../gradlew", // use root gradlew
                    ":serde:embedSwiftExportForXcode",
                    "--no-configuration-cache",
                    "--no-daemon",
                    "--console=plain",
                )
                environment(
                    mapOf(
                        "BUILT_PRODUCTS_DIR" to swiftBuildDir,
                        "TARGET_BUILD_DIR" to swiftBuildDir,
                        "SDK_NAME" to "macosx",
                        "CONFIGURATION" to "Debug",
                        "ARCHS" to "arm64",
                        "FRAMEWORKS_FOLDER_PATH" to "Frameworks",
                        "MACOSX_DEPLOYMENT_TARGET" to "14.0",
                        "DEPLOYMENT_TARGET_SETTING_NAME" to "MACOSX_DEPLOYMENT_TARGET",
                    ),
                )
            }.assertNormalExitValue()

        val generatedPackageSwift =
            layout.buildDirectory
                .file("SPMPackage/macosArm64/Debug/Package.swift")
                .get()
                .asFile
        if (generatedPackageSwift.exists()) {
            val text = generatedPackageSwift.readText()
            if (!text.contains("platforms:")) {
                generatedPackageSwift.writeText(
                    text.replaceFirst(
                        Regex("(name:\\s*\"[^\"]*\",)"),
                        "\$1\n    platforms: [.macOS(.v14)],",
                    ),
                )
            }
        }

        execOperations
            .exec {
                workingDir = layout.projectDirectory.dir("../swift-test-harness").asFile
                commandLine("swift", "package", "reset")
            }.assertNormalExitValue()

        execOperations
            .exec {
                workingDir = layout.projectDirectory.dir("../swift-test-harness").asFile
                commandLine("swift", "test")
            }.assertNormalExitValue()
    }
}

tasks.register("test") {
    dependsOn("swiftExportSmokeTest")
    // dependsOn("hostTests") if needed
}
