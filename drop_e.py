import os
import re

def process_file(filepath):
    if not os.path.exists(filepath):
        return
    with open(filepath, 'r') as f:
        content = f.read()
    
    original_content = content
    
    # 1. Remove generic declarations: <Ok, E : SerError> -> <Ok>
    content = re.sub(r'<\s*Ok\s*,\s*E\s*:\s*SerError\s*>', '<Ok>', content)
    # 2. Remove generic declarations: <Ok, E : Error> -> <Ok> (just in case)
    content = re.sub(r'<\s*Ok\s*,\s*E\s*:\s*Error\s*>', '<Ok>', content)
    # 3. Remove E from where clauses: where E : SerError -> empty
    content = re.sub(r'where\s+E\s*:\s*SerError\s*,\n\s*', 'where ', content)
    content = re.sub(r'where\s+E\s*:\s*SerError\s*', '', content)
    content = re.sub(r'where\s+E\s*:\s*Error\s*,\n\s*', 'where ', content)
    content = re.sub(r'where\s+E\s*:\s*Error\s*', '', content)
    # If the `where` keyword is left dangling (e.g. `where =`), remove `where`
    content = re.sub(r'where\s*=', '=', content)
    content = re.sub(r'where\s*\{', '{', content)
    content = re.sub(r'where\s*\n', '\n', content)
    content = re.sub(r'where\s*$', '', content, flags=re.MULTILINE)
    
    # 4. Remove E from type usages: Serializer<Ok, E> -> Serializer<Ok>
    content = re.sub(r'Serializer\s*<\s*Ok\s*,\s*E\s*>', 'Serializer<Ok>', content)
    content = re.sub(r'SerializeSeq\s*<\s*Ok\s*,\s*E\s*>', 'SerializeSeq<Ok>', content)
    content = re.sub(r'SerializeTuple\s*<\s*Ok\s*,\s*E\s*>', 'SerializeTuple<Ok>', content)
    content = re.sub(r'SerializeTupleStruct\s*<\s*Ok\s*,\s*E\s*>', 'SerializeTupleStruct<Ok>', content)
    content = re.sub(r'SerializeTupleVariant\s*<\s*Ok\s*,\s*E\s*>', 'SerializeTupleVariant<Ok>', content)
    content = re.sub(r'SerializeMap\s*<\s*Ok\s*,\s*E\s*>', 'SerializeMap<Ok>', content)
    content = re.sub(r'SerializeStruct\s*<\s*Ok\s*,\s*E\s*>', 'SerializeStruct<Ok>', content)
    content = re.sub(r'SerializeStructVariant\s*<\s*Ok\s*,\s*E\s*>', 'SerializeStructVariant<Ok>', content)
    
    # Also handle specific types in Impls.kt: Serializer<Unit, E> -> Serializer<Unit>
    content = re.sub(r'Serializer\s*<\s*Unit\s*,\s*E\s*>', 'Serializer<Unit>', content)
    content = re.sub(r'Serializer\s*<\s*String\s*,\s*E\s*>', 'Serializer<String>', content)
    
    # Also handle fun <E : SerError> -> fun
    content = re.sub(r'fun\s*<\s*E\s*:\s*SerError\s*>', 'fun', content)
    content = re.sub(r'fun\s*<\s*E\s*:\s*Error\s*>', 'fun', content)
    
    if content != original_content:
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"Updated {filepath}")

for root, dirs, files in os.walk('src/commonMain/kotlin'):
    for file in files:
        if file.endswith('.kt'):
            process_file(os.path.join(root, file))
