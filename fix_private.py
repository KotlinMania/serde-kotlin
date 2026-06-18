import os
import re

def process_file(filepath, new_name):
    if not os.path.exists(filepath):
        return
    with open(filepath, 'r') as f:
        content = f.read()
    
    new_content = re.sub(r'\bError\b', new_name, content)
    
    if new_content != content:
        with open(filepath, 'w') as f:
            f.write(new_content)
        print(f"Updated {filepath}")

process_file('src/commonMain/kotlin/io/github/kotlinmania/serde/private/Ser.kt', 'SerError')
process_file('src/commonMain/kotlin/io/github/kotlinmania/serde/private/De.kt', 'DeError')
