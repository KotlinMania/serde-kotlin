import os
import glob
import re

def replace_in_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    
    new_content = content
    new_content = new_content.replace('package io.github.kotlinmania.serde.core.`private`', 'package io.github.kotlinmania.serde.core.priv')
    new_content = new_content.replace('io.github.kotlinmania.serde.core.`private`', 'io.github.kotlinmania.serde.core.priv')
    
    new_content = new_content.replace('package io.github.kotlinmania.serde.`private`', 'package io.github.kotlinmania.serde.priv')
    new_content = new_content.replace('io.github.kotlinmania.serde.`private`', 'io.github.kotlinmania.serde.priv')
    new_content = new_content.replace('import io.github.kotlinmania.serde.private.', 'import io.github.kotlinmania.serde.priv.')
    
    if new_content != content:
        with open(filepath, 'w') as f:
            f.write(new_content)
        print(f"Updated {filepath}")

for root, _, files in os.walk('src/commonMain/kotlin'):
    for file in files:
        if file.endswith('.kt'):
            replace_in_file(os.path.join(root, file))

os.rename('src/commonMain/kotlin/io/github/kotlinmania/serde/core/private', 'src/commonMain/kotlin/io/github/kotlinmania/serde/core/priv')
os.rename('src/commonMain/kotlin/io/github/kotlinmania/serde/private', 'src/commonMain/kotlin/io/github/kotlinmania/serde/priv')

