import re

for file, replacement in [("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/Ser.kt", "SerContent"), ("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/De.kt", "DeContent")]:
    with open(file, "r") as f:
        content = f.read()
    
    # Replace `private sealed interface Content` with `internal sealed interface Replacement`
    content = re.sub(r'private sealed interface Content', f'internal sealed interface {replacement}', content)
    
    # Replace `Content` with `Replacement` globally (being careful about word boundaries)
    content = re.sub(r'\bContent\b', replacement, content)
    
    with open(file, "w") as f:
        f.write(content)
