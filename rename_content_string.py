import os
import re

def replace_in_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    
    new_content = re.sub(r'\bContent\.String\b', 'Content.StringValue', content)
    
    if new_content != content:
        with open(filepath, 'w') as f:
            f.write(new_content)
        print(f"Updated {filepath}")

for root, _, files in os.walk('src/commonMain/kotlin'):
    for file in files:
        if file.endswith('.kt'):
            replace_in_file(os.path.join(root, file))

