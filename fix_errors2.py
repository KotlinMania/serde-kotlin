import os
import re

def process_file(filepath, new_name):
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

# For private package, we need to know whether it's SerError or DeError!
# Actually, let's just see how many files have `Error` in `private`
