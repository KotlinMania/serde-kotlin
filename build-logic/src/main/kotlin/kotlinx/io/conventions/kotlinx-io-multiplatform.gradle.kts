import kotlinx.io.build.configureJava9ModuleInfoCompilation
import org.gradle.api.GradleException
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import kotlin.jvm.optionals.getOrNull

plugins {
    kotlin("multiplatform")
    kotlin("plugin.power-assert")
    id("com.android.kotlin.multiplatform.library")
    id("kotlinx-io-clean")
}

val kotlinVersion = providers.gradleProperty("versions.kotlin").getOrElse("2.3.21")
val isCodeqlBuild = providers.gradleProperty("kotlinmania.codeql").map(String::toBoolean).getOrElse(false)
val jvmToolchainVersion = providers.gradleProperty("jvm.toolchain").getOrElse("21").toInt()
val projectCompileSdk = providers.gradleProperty("android.compileSdk").getOrElse("34").toInt()
val projectMinSdk = providers.gradleProperty("android.minSdk").getOrElse("24").toInt()
val frameworkName = providers.gradleProperty("project.frameworkName").getOrElse("KmIo")
val projectNamespace = providers.gradleProperty("project.namespace").getOrElse("io.github.kotlinmania.io")

val patchSwiftExportCoroutineSupport =
    tasks.register("patchSwiftExportCoroutineSupport") {
        description = "Patches generated Swift Export coroutine support so strict Kotlin warnings remain source-only."
        dependsOn(tasks.matching { it.name.endsWith("DebugSwiftExport") })
        doLast {
            val swiftExportDir =
                layout.buildDirectory
                .dir("SwiftExport")
                .get()
                .asFile
            if (!swiftExportDir.exists()) return@doLast
            swiftExportDir
                .walkTopDown()
                .filter { it.isFile && it.name == "KotlinCoroutineSupport.kt" }
                .forEach { supportFile ->
                    val text = supportFile.readText()
                    val patched =
                        text.replace(
                            "@file:kotlin.Suppress(\"DEPRECATION_ERROR\")",
                            "@file:kotlin.Suppress(\n" +
                                "    \"DEPRECATION_ERROR\",\n" +
                                "    \"OPT_IN_OVERRIDE\",\n" +
                                "    \"OPT_IN_USAGE\",\n" +
                                "    \"OPT_IN_USAGE_ERROR\",\n" +
                                "    \"OPT_IN_USAGE_FUTURE_ERROR\",\n" +
                                "    \"REDUNDANT_ELVIS_RETURN_NULL\",\n" +
                                "    \"RETURN_VALUE_NOT_USED\",\n" +
                                "    \"UNCHECKED_CAST\",\n" +
                                "    \"UNUSED_EXPRESSION\",\n" +
                                "    \"USELESS_ELVIS\",\n" +
                                ")\n" +
                                "@file:kotlin.OptIn(\n" +
                                "    kotlinx.coroutines.InternalCoroutinesApi::class,\n" +
                                "    kotlin.concurrent.atomics.ExperimentalAtomicApi::class,\n" +
                                ")",
                        ).replace(
                            "val _result = kotlinFun(arg0, _arg1)\n            _result",
                            "kotlinFun(arg0, _arg1)",
                        ).replace(
                            "if (if (cancellationCallback(true)) Unit) Unit",
                            "cancellationCallback(true).let { }",
                        ).replace(
                            "if (cancellationCallback(true)) Unit",
                            "cancellationCallback(true).let { }",
                        ).replace(
                            "\n                cancellationCallback(true)\n",
                            "\n                cancellationCallback(true).let { }\n",
                        ).replace(
                            "return state.error?.let { throw it } ?: null",
                            "{\n                    state.error?.let { throw it }\n                    return null\n                }",
                        ).replace(
                            "is State.Completed -> state.error?.let { throw it }\n                    return null",
                            "is State.Completed -> {\n                    state.error?.let { throw it }\n                    return null\n                }",
                        )
                    if (patched != text) {
                        supportFile.writeText(patched)
                    }
                }
        }
    }

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    if (name.startsWith("compileSwiftExport")) {
        dependsOn(patchSwiftExportCoroutineSupport)
        compilerOptions.allWarningsAsErrors.set(false)
    }
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        allWarningsAsErrors.set(!isCodeqlBuild)
        languageVersion.set(KotlinVersion.KOTLIN_2_4)
        apiVersion.set(KotlinVersion.KOTLIN_2_4)
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.add("-Xreturn-value-checker=full")
    }

    jvmToolchain(jvmToolchainVersion)

    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
        compilerOptions {
            jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
            jvmTarget.set(JvmTarget.fromTarget(jvmToolchainVersion.toString()))
        }

        val versionCatalog: VersionCatalog = project.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
        val mrjToolchain = versionCatalog.findVersion("multi.release.toolchain").getOrNull()?.requiredVersion
            ?: throw GradleException("Version 'multi.release.toolchain' is not specified in the version catalog")

        val jpmsModuleName = "$projectNamespace." +
            project.name.substringAfterLast('-')
        configureJava9ModuleInfoCompilation(
            sourceSetName = project.sourceSets.create("java9ModuleInfo") {
                java.srcDir("jvm/module")
            }.name,
            parentCompilation = compilations.getByName("main"),
            moduleName = jpmsModuleName,
            toolchainVersion = JavaLanguageVersion.of(mrjToolchain)
        )
    }

    js {
        browser {
            testTask {
                filter.setExcludePatterns("*SmokeFileTest*")
            }
        }
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            testTask {
                filter.setExcludePatterns("*SmokeFileTest*")
            }
        }
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    android {
        compileSdk = projectCompileSdk
        minSdk = projectMinSdk
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }
    }

    nativeTargets()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation(kotlin("test-junit5"))
        }
    }

    // explicitApi()
    sourceSets.configureEach {
        configureSourceSet()
    }

    applyDefaultHierarchyTemplate()

    sourceSets.findByName("androidMain")?.kotlin?.srcDir("android/src")
    sourceSets.findByName("androidHostTest")?.kotlin?.srcDir("android/test")
    sourceSets.findByName("androidDeviceTest")?.kotlin?.srcDir("android/test")

    tasks {
        val jvmJar by existing(Jar::class) {
            manifest {
                attributes(
                    "Multi-Release" to true,
                    "Implementation-Vendor" to "JetBrains",
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version,
                )
            }
            from(project.sourceSets["java9ModuleInfo"].output)
        }
    }
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
powerAssert {
    val kotlinTestFunctions = listOf(
        "assertTrue", "assertFalse",
        "assertNull", "assertNotNull",
        "assertSame", "assertNotSame",
        "assertEquals", "assertNotEquals",
        "assertIs", "assertIsNot",
        "assertIsOfType", "assertIsNotOfType",
        "assertContains",
        "assertContentEquals",
        "expect"
    ).map { "kotlin.test.$it" }

    functions.addAll(kotlinTestFunctions)
}

fun KotlinSourceSet.configureSourceSet() {
    val srcDir = if (name.endsWith("Main")) "src" else "test"
    val platform = name.dropLast(4)
    if (name == "androidMain" || name == "androidHostTest" || name == "androidDeviceTest") {
        return
    }
    kotlin.srcDir("$platform/$srcDir")
    if (name == "jvmMain") {
        resources.srcDir("$platform/resources")
    } else if (name == "jvmTest") {
        resources.srcDir("$platform/test-resources")
    }
}

private fun KotlinMultiplatformExtension.nativeTargets() {
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    tvosArm64()
    tvosSimulatorArm64()

    watchosArm64()
    watchosSimulatorArm64()
    watchosDeviceArm64()

    androidNativeArm64()
    androidNativeX64()

    linuxX64()
    linuxArm64()

    macosArm64()

    mingwX64()
}
