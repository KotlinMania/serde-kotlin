import re

for file, old_name in [("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/Ser.kt", "SerContent"), ("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/De.kt", "DeContent")]:
    with open(file, "r") as f:
        content = f.read()
    
    # Replace SerContent back to Content globally
    content = re.sub(r'\b' + old_name + r'\b', 'Content', content)
    
    if file.endswith("Ser.kt"):
        content = re.sub(r'internal sealed interface Content', 'internal sealed interface Content', content) # Ensure it's internal
    
    with open(file, "w") as f:
        f.write(content)
