import re

with open("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/De.kt", "r") as f:
    content = f.read()

# Fix MapRefDeserializer
content = re.sub(
    r'class MapRefDeserializer\(\s*private val iter: Iterator<Pair<Content, Content>>,\s*\) : Deserializer',
    r'class MapRefDeserializer(\n    private val iter: Iterator<ContentMapEntry>,\n) : Deserializer',
    content
)

# Fix flatMapTakeEntry
content = re.sub(
    r'private fun flatMapTakeEntry\(\s*entry: Pair<Content, Content>\?,\s*recognized: List<String>,\s*\): Pair<Content, Content>\? \{',
    r'private fun flatMapTakeEntry(\n    entry: ContentMapEntry?,\n    recognized: List<String>,\n): ContentMapEntry? {',
    content
)

# Fix line 311 `values.add(next)` for map
content = re.sub(
    r'(val values = ArrayList<ContentMapEntry>\(cautious<ContentMapEntry>\(hint\)\)\n            while \(true\) \{\n                val next = access\.nextEntrySeed\(new\(\), new\(\)\)\.getOrThrow\(\) \?: break\n                )values\.add\(next\)',
    r'\1values.add(ContentMapEntry(next.first, next.second))',
    content
)

# Wait, `val next = access.nextEntrySeed(new(), new()).getOrThrow() ?: break` in ContentDeserializer
# There's another one in `ContentRefDeserializer.visitMap`?
# Let's check `ContentRefDeserializer.visitMap` (should be around line 490)
content = re.sub(
    r'(val values = ArrayList<ContentMapEntry>\(cautious<ContentMapEntry>\(hint\)\)\n            while \(true\) \{\n                val next = access\.nextEntrySeed\(new\(\), new\(\)\)\.getOrThrow\(\) \?: break\n                )values\.add\(next\)',
    r'\1values.add(ContentMapEntry(next.first, next.second))',
    content
)

with open("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/De.kt", "w") as f:
    f.write(content)

