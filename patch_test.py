with open("serde/build.gradle.kts", "r") as f:
    content = f.read()

content = content.replace(
    'tasks.named("test") {\n    dependsOn("swiftExportSmokeTest")\n}',
    'tasks.register("test") {\n    dependsOn("swiftExportSmokeTest")\n    // dependsOn("hostTests") if needed\n}'
)
content = content.replace('@file:OptIn(org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl::class)', 'import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl\n@file:OptIn(ExperimentalSwiftExportDsl::class)')

with open("serde/build.gradle.kts", "w") as f:
    f.write(content)
