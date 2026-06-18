with open("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/De.kt", "r") as f:
    lines = f.readlines()

has_import = any("import io.github.kotlinmania.serde.core.priv.Content" in line for line in lines)
if not has_import:
    # Insert import after package declaration
    for i, line in enumerate(lines):
        if line.startswith("package "):
            lines.insert(i + 1, "import io.github.kotlinmania.serde.core.priv.Content\n")
            break
            
with open("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/De.kt", "w") as f:
    f.writelines(lines)
