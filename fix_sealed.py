import os
import re

def fix_file(filepath):
    if not os.path.exists(filepath):
        return
    with open(filepath, 'r') as f:
        content = f.read()
    
    orig = content
    # For Unexpected
    content = re.sub(r'sealed class Unexpected \{', 'sealed interface Unexpected {', content)
    content = re.sub(r': Unexpected\(\)', ': Unexpected', content)
    
    # For TagOrContent
    content = re.sub(r'sealed class TagOrContent \{', 'sealed interface TagOrContent {', content)
    content = re.sub(r': TagOrContent\(\)', ': TagOrContent', content)
    
    # For Content in Ser.kt
    content = re.sub(r'private sealed class Content : Serialize \{', 'private sealed interface Content : Serialize {', content)
    content = re.sub(r': Content\(\)', ': Content', content)
    
    if content != orig:
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"Updated {filepath}")

fix_file('src/commonMain/kotlin/io/github/kotlinmania/serde/core/de/Unexpected.kt')
fix_file('src/commonMain/kotlin/io/github/kotlinmania/serde/private/De.kt')
fix_file('src/commonMain/kotlin/io/github/kotlinmania/serde/private/Ser.kt')
