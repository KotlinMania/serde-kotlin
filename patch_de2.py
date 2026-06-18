import re

with open("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/De.kt", "r") as f:
    content = f.read()

# Add ContentMapEntry import
if "import io.github.kotlinmania.serde.core.priv.ContentMapEntry" not in content:
    content = content.replace(
        "import io.github.kotlinmania.serde.core.priv.Content",
        "import io.github.kotlinmania.serde.core.priv.Content\nimport io.github.kotlinmania.serde.core.priv.ContentMapEntry"
    )

# Fix visitContentMapRef and visitContentMap signatures
content = re.sub(
    r'fun <V> visitContentMapRef\(\s*map: List<Pair<Content, Content>>,\s*visitor: Visitor<V>,\s*\)',
    r'fun <V> visitContentMapRef(\n    map: List<ContentMapEntry>,\n    visitor: Visitor<V>,\n)',
    content
)
content = re.sub(
    r'fun <V> visitContentMap\(\s*map: List<Pair<Content, Content>>,\s*visitor: Visitor<V>,\s*\)',
    r'fun <V> visitContentMap(\n    map: List<ContentMapEntry>,\n    visitor: Visitor<V>,\n)',
    content
)

# Fix MapDeserializer and MapRefDeserializer
content = re.sub(
    r'class MapDeserializer\(\s*private val iter: Iterator<Pair<Content, Content>>,\s*private var value: Content\? = null,\s*\)',
    r'class MapDeserializer(\n    private val iter: Iterator<ContentMapEntry>,\n    private var value: Content? = null,\n)',
    content
)
content = re.sub(
    r'class MapRefDeserializer\(\s*private val iter: Iterator<Pair<Content, Content>>,\s*private var value: Content\? = null,\s*\)',
    r'class MapRefDeserializer(\n    private val iter: Iterator<ContentMapEntry>,\n    private var value: Content? = null,\n)',
    content
)

# Fix iter.next() for these deserializers
content = re.sub(
    r'val entry = iter\.next\(\)\s*value = entry\.second\s*seed\.deserialize\(ContentDeserializer\.new\(entry\.first\)\)',
    r'val entry = iter.next()\n            value = entry.value\n            seed.deserialize(ContentDeserializer.new(entry.key))',
    content
)
content = re.sub(
    r'val entry = iter\.next\(\)\s*value = entry\.second\s*seed\.deserialize\(ContentRefDeserializer\.new\(entry\.first\)\)',
    r'val entry = iter.next()\n            value = entry.value\n            seed.deserialize(ContentRefDeserializer.new(entry.key))',
    content
)

# Fix FlatStructAccess iter
content = re.sub(
    r'while \(iter\.hasNext\(\)\) \{\s*val index = iter\.nextIndex\(\)\s*val entry = iter\.next\(\)\s*val found = flatMapTakeEntry\(entry, fields\)',
    r'while (currentIndex < buffer.size) {\n                val index = currentIndex\n                val entry = buffer.get(currentIndex++)\n                val found = flatMapTakeEntry(entry, fields)',
    content
)

with open("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/De.kt", "w") as f:
    f.write(content)
