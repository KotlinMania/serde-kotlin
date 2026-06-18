import re

with open("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/De.kt", "r") as f:
    content = f.read()

content = re.sub(
    r'private class MapDeserializer\(\s*private val iter: Iterator<Pair<Content, Content>>,\s*\)',
    r'private class MapDeserializer(\n    private val iter: Iterator<ContentMapEntry>,\n)',
    content
)

content = re.sub(
    r'private fun nextPair\(\): Pair<Content, Content>\?',
    r'private fun nextPair(): ContentMapEntry?',
    content
)

with open("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/De.kt", "w") as f:
    f.write(content)

