import groovy.json.JsonSlurper
import io.github.kotlinmania.build.BuildToolTask
import io.github.kotlinmania.build.NativeTestRunner
import io.github.kotlinmania.build.NodeFfiBuilder
import io.github.kotlinmania.build.SwiftManifestEnforcer
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.plugins.signing.Sign
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.web.nodejs.BaseNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.web.yarn.BaseYarnRootExtension
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Base64
import java.util.UUID
import java.util.zip.ZipInputStream
import org.gradle.api.publish.PublishingExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

buildscript {
    dependencies {
        classpath(files(rootProject.buildscript.configurations.getByName("classpath")))
    }
}

// Project-owned runtime checks remain callable without duplicating template infrastructure.
val kotlin = extensions.getByType(KotlinMultiplatformExtension::class.java)
val publishing = extensions.getByType(PublishingExtension::class.java)
val fullTargetBuildTaskNames =
    (rootProject.extra["template.fullTargetBuildTaskNames"] as Iterable<*>).map { it.toString() }
val isWindowsHost = System.getProperty("os.name").lowercase().contains("windows")
val isMacHost = System.getProperty("os.name").lowercase().contains("mac")
val projectCompileSdk = providers.gradleProperty("android.compileSdk").getOrElse("34")
val projectAndroidSdkDir = layout.projectDirectory.dir(".android-sdk").asFile
fun optionalTrimmedProperty(name: String): String? =
    providers.gradleProperty(name).orNull?.trim()?.takeIf { it.isNotEmpty() }

val publicationCompilationTasks =
    kotlin.targets
        .filter { it.name != "metadata" }
        .flatMap { target -> target.compilations.map { it.compileTaskProvider } }

val compileAllTargets =
    tasks.register("compileAllTargets") {
        group = "verification"
        description = "Compiles main and test sources for every declared target."
        dependsOn(publicationCompilationTasks)
        doLast {
            val skippedCompilations =
                publicationCompilationTasks.map { it.get() }.filter { task ->
                    !task.state.executed ||
                        (task.state.skipped && !task.state.upToDate && task.state.skipMessage != "FROM-CACHE")
                }
            check(skippedCompilations.isEmpty()) {
                "Required target compilations did not complete: " +
                    skippedCompilations.joinToString { "${it.path} (${it.state.skipMessage ?: "not executed"})" }
            }
        }
    }

val buildAllTargets =
    tasks.register("buildAllTargets") {
        group = "verification"
        description = "Compiles and links every declared target before runtime validation."
        dependsOn(compileAllTargets, fullTargetBuildTaskNames.filter { it != "swiftExportSmokeTest" })
    }

tasks.withType<AbstractTestTask>().configureEach {
    mustRunAfter(buildAllTargets)
}
tasks.named("swiftExportSmokeTest") { mustRunAfter(buildAllTargets) }

val publicationPomChecks =
    publishing.publications.withType<MavenPublication>().map { publication ->
        tasks.named("checkPomFileFor${publication.name.replaceFirstChar { it.uppercase() }}Publication")
    }

val checkPublicationPoms =
    tasks.register("checkPublicationPoms") {
        group = "verification"
        description = "Checks the generated POM for every Maven publication."
        dependsOn(publicationPomChecks)
        doLast {
            for (provider in publicationPomChecks) {
                val task = provider.get()
                task.state.rethrowFailure()
                check(
                    task.state.executed &&
                        (!task.state.skipped || task.state.upToDate || task.state.skipMessage == "FROM-CACHE"),
                ) {
                    "Required POM check did not execute: ${task.path} (${task.state.skipMessage})"
                }
            }
        }
    }

val publicationRuntimeTaskNames =
    listOf(
        "jvmTest",
        "testAndroidHostTest",
        "jsBrowserTest",
        "jsNodeTest",
        "wasmJsBrowserTest",
        "wasmJsNodeTest",
        "wasmWasiNodeTest",
        "macosArm64Test",
        "iosSimulatorArm64Test",
        "tvosSimulatorArm64Test",
        "watchosSimulatorArm64Test",
    )

val foreignNativeRuntimeTasks =
    listOf("linuxArm64", "linuxX64", "mingwX64").map { target ->
        tasks.register<BuildToolTask>("validate${target.replaceFirstChar { it.uppercase() }}Runtime") {
            group = "verification"
            description = "Runs linked tests on a local native host or existing Parallels guest; records unavailable hosts."
            dependsOn("linkDebugTest${target.replaceFirstChar { it.uppercase() }}")
            mustRunAfter(buildAllTargets)
            toolClass.set(NativeTestRunner::class.java.name)
            val executable = if (target == "mingwX64") "test.exe" else "test.kexe"
            val platform = if (target == "mingwX64") "windows" else "linux"
            arguments.addAll(
                "--target",
                target,
                "--binary",
                layout.buildDirectory
                    .file("bin/$target/debugTest/$executable")
                    .get()
                    .asFile.absolutePath,
                "--report-dir",
                layout.buildDirectory
                    .dir("reports/local-native-tests")
                    .get()
                    .asFile.absolutePath,
                "--allow-unavailable",
            )
            providers
                .gradleProperty("project.validation.$platform.vm")
                .orNull
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { arguments.addAll("--vm", it) }
        }
    }

tasks.matching { it.name in publicationRuntimeTaskNames }.configureEach {
    outputs.upToDateWhen { false }
    outputs.doNotCacheIf("Publication requires actual runtime execution") { true }
}

val verifyPublicationRuntimes =
    tasks.register("verifyPublicationRuntimes") {
        group = "verification"
        description = "Rejects skipped required test tasks and missing, empty or failing runtime reports."
        dependsOn(buildAllTargets, publicationRuntimeTaskNames, foreignNativeRuntimeTasks)
        doLast {
            for (provider in foreignNativeRuntimeTasks) {
                val task = provider.get()
                task.state.rethrowFailure()
                check(task.state.executed && !task.state.skipped) {
                    "Required native runtime did not execute: ${task.path} (${task.state.skipMessage})"
                }
            }
            for (name in publicationRuntimeTaskNames) {
                val task = tasks.named<AbstractTestTask>(name).get()
                task.state.rethrowFailure()
                check(task.state.executed && !task.state.skipped) {
                    "Required runtime did not execute: ${task.path} (${task.state.skipMessage})"
                }
                val reports =
                    fileTree(
                        task.reports.junitXml.outputLocation
                            .get()
                            .asFile,
                    ) { include("*.xml") }.files
                check(reports.isNotEmpty()) { "Missing runtime reports for ${task.path}" }
                var tests = 0
                val parser =
                    javax.xml.parsers.DocumentBuilderFactory
                        .newInstance()
                        .newDocumentBuilder()
                for (report in reports) {
                    val suite = parser.parse(report).documentElement
                    val count = suite.getAttribute("tests").toInt()
                    check(suite.getElementsByTagName("testcase").length == count) {
                        "${task.path}: test count does not match the recorded cases in ${report.name}"
                    }
                    tests += count
                    for (attribute in listOf("failures", "errors", "skipped")) {
                        check(suite.getAttribute(attribute).ifEmpty { "0" }.toInt() == 0) {
                            "${task.path}: $attribute in ${report.name}"
                        }
                    }
                    for (element in listOf("failure", "error", "skipped")) {
                        check(suite.getElementsByTagName(element).length == 0) {
                            "${task.path}: $element result in ${report.name}"
                        }
                    }
                }
                check(tests > 0) { "Required runtime ran no tests: ${task.path}" }
            }
        }
    }

val publicationCheck =
    tasks.register("publicationCheck") {
        group = "verification"
        description = "Runs the retained all-platform verification suite when explicitly requested."
        dependsOn("build", compileAllTargets, checkPublicationPoms, verifyPublicationRuntimes)
        doLast {
            for (name in fullTargetBuildTaskNames + "build") {
                val task = tasks.named(name).get()
                task.state.rethrowFailure()
                check(
                    task.state.executed &&
                        (!task.state.skipped || task.state.upToDate || task.state.skipMessage == "FROM-CACHE"),
                ) {
                    "Required build task did not execute: ${task.path} (${task.state.skipMessage})"
                }
            }
        }
    }

