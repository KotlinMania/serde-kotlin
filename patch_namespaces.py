import re

for proj in ["serde_derive", "serde_core", "serde"]:
    with open(f"{proj}/build.gradle.kts", "r") as f:
        content = f.read()
    
    # remove the appended android block
    content = re.sub(r'android\s*\{\s*namespace\s*=\s*"io.github.kotlinmania.serde[a-zA-Z0-custom.\"]*\s*\}', '', content)
    
    namespace = f'io.github.kotlinmania.serde.{proj.replace("serde_", "")}' if proj != "serde" else "io.github.kotlinmania.serde"
    
    # insert android { namespace = ... } inside kotlin block
    if "kotlin {" in content:
        content = content.replace("kotlin {", f'kotlin {{\n    android {{\n        namespace = "{namespace}"\n    }}\n', 1)
    
    with open(f"{proj}/build.gradle.kts", "w") as f:
        f.write(content)
