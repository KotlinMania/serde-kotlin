import os
import re

def process_file(filepath):
    if not os.path.exists(filepath):
        return
    with open(filepath, 'r') as f:
        content = f.read()
    
    original_content = content
    
    content = re.sub(r'Serializer<\s*Content\s*,\s*E\s*>', 'Serializer<Content>', content)
    content = re.sub(r'SerializeSeq<\s*Content\s*,\s*E\s*>', 'SerializeSeq<Content>', content)
    content = re.sub(r'SerializeTuple<\s*Content\s*,\s*E\s*>', 'SerializeTuple<Content>', content)
    content = re.sub(r'SerializeTupleStruct<\s*Content\s*,\s*E\s*>', 'SerializeTupleStruct<Content>', content)
    content = re.sub(r'SerializeTupleVariant<\s*Content\s*,\s*E\s*>', 'SerializeTupleVariant<Content>', content)
    content = re.sub(r'SerializeMap<\s*Content\s*,\s*E\s*>', 'SerializeMap<Content>', content)
    content = re.sub(r'SerializeStruct<\s*Content\s*,\s*E\s*>', 'SerializeStruct<Content>', content)
    content = re.sub(r'SerializeStructVariant<\s*Content\s*,\s*E\s*>', 'SerializeStructVariant<Content>', content)
    content = re.sub(r'where\s+E\s*:\s*Error\s*', '', content)
    
    # Also fix where clauses that were left malformed
    content = re.sub(r'where\s*\n\s*\{', '{', content)
    content = re.sub(r'where\s*\{', '{', content)
    content = re.sub(r'where\s*$', '', content, flags=re.MULTILINE)
    
    # Check for other random `<..., E>` generics in Ser.kt
    # We can just look for `, E>`
    content = re.sub(r'<\s*([^>]+)\s*,\s*E\s*>', r'<\1>', content)
    
    if content != original_content:
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"Updated {filepath}")

process_file('src/commonMain/kotlin/io/github/kotlinmania/serde/private/Ser.kt')
