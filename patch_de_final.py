import re

with open("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/De.kt", "r") as f:
    content = f.read()

# Fix contentClone in Content.Map
content = re.sub(
    r'is Content\.Map -> Content\.Map\(content\.value\.map \{ \(k, v\) -> contentClone\(k\) to contentClone\(v\) \}\)',
    r'is Content.Map -> Content.Map(content.value.map { entry -> ContentMapEntry(contentClone(entry.key), contentClone(entry.value)) })',
    content
)

# Fix map: List<Pair<Content, Content>> for ContentDeserializer.new etc.
content = re.sub(
    r'fun new\(content: List<Pair<Content, Content>>\)',
    r'fun new(content: List<ContentMapEntry>)',
    content
)

content = re.sub(
    r'content: List<Pair<Content, Content>>,',
    r'content: List<ContentMapEntry>,',
    content
)

# Fix values.add(something to something) -> values.add(ContentMapEntry(something, something))
content = re.sub(
    r'values\.add\((\w+(?:\.\w+)?)\s*to\s*(\w+)\)',
    r'values.add(ContentMapEntry(\1, \2))',
    content
)

# Fix nextPair() -> ContentMapEntry?
content = re.sub(
    r'private fun nextPair\(\): Pair<Content, Content>\?',
    r'private fun nextPair(): ContentMapEntry?',
    content
)

# Fix `val (key, value) = found` to `val key = found.key; val value = found.value` (if any missed)
# And `found.first` to `found.key`
content = content.replace("found.first", "found.key")
content = content.replace("found.second", "found.value")

# Fix `key to value` in nextPair()
content = re.sub(
    r'key to value',
    r'ContentMapEntry(key, value)',
    content
)

# Fix iter.next() for FlatStructAccess (line 1918 Unresolved reference 'iter')
# Wait, I already updated FlatStructAccess in patch_de2.py, but line 1918 might be FlatMapAccess!
# Let's check FlatMapAccess!
content = re.sub(
    r'while \(iter\.hasNext\(\)\) \{\s*val entry = iter\.next\(\)\s*if \(entry != null\) \{',
    r'while (currentIndex < buffer.size) {\n                val entry = buffer.get(currentIndex++)\n                if (entry != null) {',
    content
)
# And replace iter references inside FlatMapAccess and FlatStructAccess!
# Actually, I'll just change FlatMapAccess back if it was messed up.

with open("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/De.kt", "w") as f:
    f.write(content)

