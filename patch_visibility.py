import re

def process_file(path):
    with open(path, 'r') as f:
        content = f.read()

    # Match `class Name`, `fun Name`, `interface Name` at the start of a line and make them `internal`
    # if they are not already private or internal.
    
    # We'll just replace `^class ` with `internal class `, `^fun ` with `internal fun `, `^interface ` with `internal interface `
    content = re.sub(r'^class ', 'internal class ', content, flags=re.MULTILINE)
    content = re.sub(r'^fun ', 'internal fun ', content, flags=re.MULTILINE)
    content = re.sub(r'^interface ', 'internal interface ', content, flags=re.MULTILINE)

    with open(path, 'w') as f:
        f.write(content)

process_file("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/Ser.kt")
process_file("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/De.kt")

