import os
import re

def process_file(filepath):
    if not os.path.exists(filepath):
        return
    with open(filepath, 'r') as f:
        content = f.read()
    
    original_content = content
    
    # Fmt.kt fixes
    content = re.sub(r'FmtError', 'SerdeError', content)
    content = re.sub(r'SerializeTuple<Ok, SerdeError>', 'SerializeTuple<Ok>', content)
    content = re.sub(r'SerializeTupleStruct<Ok, SerdeError>', 'SerializeTupleStruct<Ok>', content)
    content = re.sub(r'SerializeTupleVariant<Ok, SerdeError>', 'SerializeTupleVariant<Ok>', content)
    content = re.sub(r'SerializeMap<Ok, SerdeError>', 'SerializeMap<Ok>', content)
    content = re.sub(r'SerializeStruct<Ok, SerdeError>', 'SerializeStruct<Ok>', content)
    content = re.sub(r'SerializeStructVariant<Ok, SerdeError>', 'SerializeStructVariant<Ok>', content)
    content = re.sub(r'SerializeSeq<Ok, SerdeError>', 'SerializeSeq<Ok>', content)
    
    # Ser.kt fixes
    content = re.sub(r'TaggedSerializer<\s*Ok\s*,\s*E\s*,\s*S\s*>', 'TaggedSerializer<Ok, S>', content)
    content = re.sub(r'SerializeTupleVariantAsMapValue<\s*Ok\s*,\s*E\s*,\s*M\s*>', 'SerializeTupleVariantAsMapValue<Ok, M>', content)
    content = re.sub(r'SerializeStructVariantAsMapValue<\s*Ok\s*,\s*E\s*,\s*M\s*>', 'SerializeStructVariantAsMapValue<Ok, M>', content)
    
    # More general cleanups
    content = re.sub(r'class Error\([^)]*\)\s*:\s*RuntimeException\([^)]*\)', '', content)
    content = re.sub(r'class SerdeError\([^)]*\)\s*:\s*RuntimeException\([^)]*\)', '', content)

    # In Doc.kt, Fmt.kt, replace error constructors
    content = re.sub(r'SerdeError\("fmt error"\)', 'SerdeError("fmt error")', content)
    
    if content != original_content:
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"Updated {filepath}")

for root, dirs, files in os.walk('src/commonMain/kotlin'):
    for file in files:
        if file.endswith('.kt'):
            process_file(os.path.join(root, file))
