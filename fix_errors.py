import os
import re

def process_file(filepath, old_name, new_name):
    if not os.path.exists(filepath):
        return
    with open(filepath, 'r') as f:
        content = f.read()
    
    # We want to replace all occurrences of Error with new_name,
    # but only as a whole word.
    new_content = re.sub(r'\bError\b', new_name, content)
    
    if new_content != content:
        with open(filepath, 'w') as f:
            f.write(new_content)
        print(f"Updated {filepath}")

# For de package
for root, dirs, files in os.walk('src/commonMain/kotlin/io/github/kotlinmania/serde/core/de'):
    for file in files:
        if file.endswith('.kt'):
            process_file(os.path.join(root, file), 'Error', 'DeError')

# For ser package
for root, dirs, files in os.walk('src/commonMain/kotlin/io/github/kotlinmania/serde/core/ser'):
    for file in files:
        if file.endswith('.kt'):
            process_file(os.path.join(root, file), 'Error', 'SerError')

# There are also files in serde/private/ that use `Error` which referred to ser.Error or de.Error!
# Wait, if they imported `io.github.kotlinmania.serde.core.ser.Error`, how was it imported?
# Usually as `import io.github.kotlinmania.serde.core.ser.Error`!
