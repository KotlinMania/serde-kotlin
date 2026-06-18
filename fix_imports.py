import os
import re

def process_file(filepath):
    if not os.path.exists(filepath):
        return
    with open(filepath, 'r') as f:
        content = f.read()
    
    original_content = content
    
    if "SerdeError" in content and "import io.github.kotlinmania.serde.SerdeError" not in content:
        content = re.sub(r'(package [^\n]+)\n', r'\1\n\nimport io.github.kotlinmania.serde.SerdeError\n', content)
        
    if "SerdeResult" in content and "import io.github.kotlinmania.serde.SerdeResult" not in content:
        content = re.sub(r'(package [^\n]+)\n', r'\1\n\nimport io.github.kotlinmania.serde.SerdeResult\n', content)

    # In Doc.kt, Fmt.kt, replace `class Error ... : SerdeError`
    content = re.sub(r'class Error\([^)]*\)\s*:\s*RuntimeException\([^)]*\)\s*,\s*SerdeError', 'class Error(message: String = "") : RuntimeException(message)', content)
    content = re.sub(r'class FmtError\([^)]*\)\s*:\s*RuntimeException\([^)]*\)\s*,\s*SerdeError', 'class FmtError(message: String = "") : RuntimeException(message)', content)
    content = re.sub(r'class FmtError\([^)]*\)\s*:\s*SerdeError', 'class FmtError(val msg: String = "")', content)
    
    # In Value.kt, delete `class SerdeError ... { ... }`
    content = re.sub(r'class SerdeError private constructor\([^}]+\}\n\s*\}\n\s*override fun toString\(\): String = err\n\}', '', content)
    # also with any trailing space
    content = re.sub(r'class SerdeError private constructor[^{]+\{[^}]+\}[^}]+\}', '', content)
    
    # Fmt.kt: Argument type mismatch: actual type is 'FormatterSerializer', but 'Serializer<Unit>' was expected.
    # It must be `class FormatterSerializer : Serializer<Unit>`
    content = re.sub(r'class FormatterSerializer\s*:\s*Serializer\s*<', 'class FormatterSerializer : Serializer<', content)
    
    if content != original_content:
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"Updated {filepath}")

for root, dirs, files in os.walk('src/commonMain/kotlin'):
    for file in files:
        if file.endswith('.kt'):
            process_file(os.path.join(root, file))
