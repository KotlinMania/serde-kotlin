import re
with open("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/De.kt", "r") as f:
    content = f.read()

content = re.sub(r'Content\.String\b', 'Content.StringValue', content)

with open("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/De.kt", "w") as f:
    f.write(content)
