import re

with open("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/De.kt", "r") as f:
    content = f.read()

# Fix Error.invalidType -> SerdeError.invalidType
content = re.sub(r'\bError\.invalidType\b', 'SerdeError.invalidType', content)
content = re.sub(r'\bError\.invalidLength\b', 'SerdeError.invalidLength', content)
content = re.sub(r'\bError\.invalidValue\b', 'SerdeError.invalidValue', content)

# Fix Content.String -> Content.StringValue
content = re.sub(r'\bContent\.String\b', 'Content.StringValue', content)

# Fix first.first to first.second -> first.key to first.value
content = re.sub(r'\bfirst\.first to first\.second\b', 'first.key to first.value', content)

# Fix visitContentMapRef and visitContentMap signatures
content = re.sub(
    r'visitContentMapRef\(\s*content: List<Pair<Content, Content>>,\s*visitor: Visitor<V>,\s*\)',
    r'visitContentMapRef(\n    content: List<ContentMapEntry>,\n    visitor: Visitor<V>,\n)',
    content
)
content = re.sub(
    r'visitContentMap\(\s*content: List<Pair<Content, Content>>,\s*visitor: Visitor<V>,\s*\)',
    r'visitContentMap(\n    content: List<ContentMapEntry>,\n    visitor: Visitor<V>,\n)',
    content
)

# Update MapDeserializer to take Iterator<ContentMapEntry>
content = re.sub(
    r'class MapDeserializer\(\s*private val iter: Iterator<Pair<Content, Content>>,\s*private var value: Content\? = null,\s*\) : MapAccess \{',
    r'class MapDeserializer(\n    private val iter: Iterator<ContentMapEntry>,\n    private var value: Content? = null,\n) : MapAccess {',
    content
)

# Update nextPair() to return ContentMapEntry?
content = re.sub(
    r'private fun nextPair\(\): Pair<Content, Content>\?',
    r'private fun nextPair(): ContentMapEntry?',
    content
)

# Replace ContentMapEntry(key, value) inside MapDeserializer.nextEntrySeed with `key to value` if it's there?
# Wait! In the current De.kt, nextEntrySeed is STILL using `key to value` because I RESTORED it and ran `patch_de_clean.py`!
# So `key to value` is ALREADY there! And it correctly returns Pair<K, V>?

# Fix content.value.map { (k, v) -> ... }
content = re.sub(
    r'is Content\.Map -> Content\.Map\(content\.value\.map \{ \(k, v\) -> contentClone\(k\) to contentClone\(v\) \}\)',
    r'is Content.Map -> Content.Map(content.value.map { entry -> ContentMapEntry(contentClone(entry.key), contentClone(entry.value)) })',
    content
)

# Fix ContentDeserializer.new
content = re.sub(
    r'fun new\(content: List<Pair<Content, Content>>\)',
    r'fun new(content: List<ContentMapEntry>)',
    content
)

# ArrayList / cautious Pair replacement
content = content.replace("ArrayList<Pair<Content, Content>>", "ArrayList<ContentMapEntry>")
content = content.replace("cautious<Pair<Content, Content>>", "cautious<ContentMapEntry>")

# And values.add
content = re.sub(
    r'values\.add\((\w+(?:\.\w+)?)\s*to\s*(\w+)\)',
    r'values.add(ContentMapEntry(\1, \2))',
    content
)

# Ensure Pair.key / Pair.value is not generated where ContentMapEntry isn't.
# Wait, `pair.first` -> `pair.key`
content = re.sub(r'\bpair\.first\b', 'pair.key', content)
content = re.sub(r'\bpair\.second\b', 'pair.value', content)

with open("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/De.kt", "w") as f:
    f.write(content)

