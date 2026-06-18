import os
import re

def process_file(filepath):
    if not os.path.exists(filepath):
        return
    with open(filepath, 'r') as f:
        content = f.read()
    
    original_content = content
    
    # Replace DeError and SerError with SerdeError
    content = re.sub(r'\bDeError\b', 'SerdeError', content)
    content = re.sub(r'\bSerError\b', 'SerdeError', content)
    
    # Remove unused imports
    content = re.sub(r'import io\.github\.kotlinmania\.serde\.core\.de\.DeError\n?', '', content)
    content = re.sub(r'import io\.github\.kotlinmania\.serde\.core\.ser\.SerError\n?', '', content)
    
    # Also fix some lingering `<E>` from ContentSerializer or others in Ser.kt
    content = re.sub(r'<\s*E\s*>', '', content)
    
    if content != original_content:
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"Updated {filepath}")

for root, dirs, files in os.walk('src/commonMain/kotlin'):
    for file in files:
        if file.endswith('.kt'):
            process_file(os.path.join(root, file))
