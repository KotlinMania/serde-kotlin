with open("serde/build.gradle.kts", "r") as f:
    content = f.read()

imports = """
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.process.ExecOperations
@file:OptIn(org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl::class)
"""

if "import org.gradle.kotlin.dsl.support.serviceOf" not in content:
    content = imports + "\n" + content

with open("serde/build.gradle.kts", "w") as f:
    f.write(content)
