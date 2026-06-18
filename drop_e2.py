import os
import re

def process_file(filepath):
    if not os.path.exists(filepath):
        return
    with open(filepath, 'r') as f:
        content = f.read()
    
    original_content = content
    
    content = re.sub(r'SerializeSeq<\s*Unit\s*,\s*E\s*>', 'SerializeSeq<Unit>', content)
    content = re.sub(r'SerializeTuple<\s*Unit\s*,\s*E\s*>', 'SerializeTuple<Unit>', content)
    content = re.sub(r'SerializeTupleStruct<\s*Unit\s*,\s*E\s*>', 'SerializeTupleStruct<Unit>', content)
    content = re.sub(r'SerializeTupleVariant<\s*Unit\s*,\s*E\s*>', 'SerializeTupleVariant<Unit>', content)
    content = re.sub(r'SerializeMap<\s*Unit\s*,\s*E\s*>', 'SerializeMap<Unit>', content)
    content = re.sub(r'SerializeStruct<\s*Unit\s*,\s*E\s*>', 'SerializeStruct<Unit>', content)
    content = re.sub(r'SerializeStructVariant<\s*Unit\s*,\s*E\s*>', 'SerializeStructVariant<Unit>', content)
    
    content = re.sub(r'SerializeMap<\s*MOk\s*,\s*E\s*>', 'SerializeMap<MOk>', content)
    content = re.sub(r'FlatMapSerializeMap<\s*MOk\s*,\s*E\s*,\s*M\s*>', 'FlatMapSerializeMap<MOk, M>', content)
    content = re.sub(r'FlatMapSerializeStruct<\s*MOk\s*,\s*E\s*,\s*M\s*>', 'FlatMapSerializeStruct<MOk, M>', content)
    content = re.sub(r'FlatMapSerializeTupleVariantAsMapValue<\s*MOk\s*,\s*E\s*,\s*M\s*>', 'FlatMapSerializeTupleVariantAsMapValue<MOk, M>', content)
    content = re.sub(r'FlatMapSerializeStructVariantAsMapValue<\s*MOk\s*,\s*E\s*,\s*M\s*>', 'FlatMapSerializeStructVariantAsMapValue<MOk, M>', content)
    
    content = re.sub(r'ContentSerializer<\s*E\s*>', 'ContentSerializer', content)
    
    content = re.sub(r'<\s*Ok\s*,\s*E\s*>', '<Ok>', content)
    content = re.sub(r'<\s*Unit\s*,\s*E\s*>', '<Unit>', content)
    content = re.sub(r'<\s*T\s*,\s*E\s*>', '<T>', content)
    content = re.sub(r'<\s*String\s*,\s*E\s*>', '<String>', content)
    
    if content != original_content:
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"Updated {filepath}")

for root, dirs, files in os.walk('src/commonMain/kotlin'):
    for file in files:
        if file.endswith('.kt'):
            process_file(os.path.join(root, file))
