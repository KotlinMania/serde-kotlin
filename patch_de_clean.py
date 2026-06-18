import re

with open("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/De.kt", "r") as f:
    content = f.read()

# 1 & 2: Packages and imports
content = content.replace(
    "package io.github.kotlinmania.serde.private",
    "package io.github.kotlinmania.serde.priv"
)
content = content.replace(
    "import io.github.kotlinmania.serde.core.private.Content",
    "import io.github.kotlinmania.serde.core.priv.Content\nimport io.github.kotlinmania.serde.core.priv.ContentMapEntry"
)

# 8: Remove where F : Any
content = re.sub(r'\) : DeserializeSeed<F>\s*where F : Any \{', r') : DeserializeSeed<F> {', content)
content = re.sub(r'\) : Visitor<F>\s*where F : Any \{', r') : Visitor<F> {', content)

# 4: Insert FlatMapBuffer
buffer_class = """
class FlatMapBuffer(
    @PublishedApi internal val entries: MutableList<ContentMapEntry?> = mutableListOf()
) {
    val size: Int get() = entries.size
    fun get(index: Int): ContentMapEntry? = entries[index]
    fun setNull(index: Int) { entries[index] = null }
    fun add(key: Content, value: Content) { entries.add(ContentMapEntry(key, value)) }
}
"""
content = re.sub(r'class FlatMapDeserializer\(', buffer_class + '\nclass FlatMapDeserializer(', content)

# 5: Update FlatMapDeserializer signature
content = re.sub(
    r'class FlatMapDeserializer\(\s*val entries: MutableList<Pair<Content, Content>\?>,\s*\)',
    r'class FlatMapDeserializer(\n    val buffer: FlatMapBuffer,\n)',
    content
)

# Fix FlatMapDeserializer loops
content = re.sub(
    r'for \(index in entries\.indices\) \{\s*val entry = entries\[index\]',
    r'for (index in 0 until buffer.size) {\n                val entry = buffer.get(index)',
    content
)
content = re.sub(
    r'val \(key, value\) = found',
    r'val key = found.key; val value = found.value',
    content
)

# Update deserializeMap
content = re.sub(
    r'FlatMapAccess\(\s*iter = entries\.iterator\(\),\s*\)',
    r'FlatMapAccess(\n                buffer = buffer,\n            )',
    content
)

# Update deserializeStruct
content = re.sub(
    r'FlatStructAccess\(\s*iter = entries\.listIterator\(\),\s*fields = fields,\s*\)',
    r'FlatStructAccess(\n                buffer = buffer,\n                fields = fields,\n            )',
    content
)

# 6: Update FlatMapAccess
content = re.sub(
    r'private class FlatMapAccess\(\s*private val iter: Iterator<Pair<Content, Content>\?>,\s*\) : MapAccess \{',
    r'private class FlatMapAccess(\n    private val buffer: FlatMapBuffer,\n) : MapAccess {\n    private var currentIndex = 0',
    content
)
content = re.sub(
    r'for \(item in iter\) \{\s*// Items in the vector are nulled out when used by a data structure\.\s*val entry = item \?: continue',
    r'while (currentIndex < buffer.size) {\n                val entry = buffer.get(currentIndex++) ?: continue',
    content
)
content = re.sub(r'pendingContent = entry\.second', r'pendingContent = entry.value', content)
content = re.sub(r'ContentRefDeserializer\.new\(entry\.first\)', r'ContentRefDeserializer.new(entry.key)', content)


# 6: Update FlatStructAccess
content = re.sub(
    r'private class FlatStructAccess\(\s*private val iter: MutableListIterator<Pair<Content, Content>\?>,\s*private val fields: List<String>,\s*\) : MapAccess \{',
    r'private class FlatStructAccess(\n    private val buffer: FlatMapBuffer,\n    private val fields: List<String>,\n) : MapAccess {\n    private var currentIndex = 0',
    content
)
content = re.sub(
    r'while \(iter\.hasNext\(\)\) \{\s*val index = iter\.nextIndex\(\)\s*val entry = iter\.next\(\)\s*val found = flatMapTakeEntry\(entry, fields\)',
    r'while (currentIndex < buffer.size) {\n                val index = currentIndex\n                val entry = buffer.get(currentIndex++)\n                val found = flatMapTakeEntry(entry, fields)',
    content
)
content = re.sub(r'pendingContent = entry\.second', r'pendingContent = entry.value', content)
content = re.sub(r'ContentDeserializer\.new\(entry\.first\)', r'ContentDeserializer.new(entry.key)', content)
# Fix the found destructuring in FlatStructAccess if it exists!
content = re.sub(
    r'val key = found\.first',
    r'val key = found.key',
    content
)
content = re.sub(
    r'val value = found\.second',
    r'val value = found.value',
    content
)
content = re.sub(r'iter\.set\(null\)', r'buffer.setNull(index)', content)

# 7: Update flatMapTakeEntry
content = re.sub(
    r'private fun flatMapTakeEntry\(\s*entry: Pair<Content, Content>\?,\s*recognized: List<String>,\s*\): Pair<Content, Content>\? =',
    r'private fun flatMapTakeEntry(\n    entry: ContentMapEntry?,\n    recognized: List<String>,\n): ContentMapEntry? =',
    content
)
content = re.sub(r'val name = contentAsStr\(entry\.first\)', r'val name = contentAsStr(entry.key)', content)


with open("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/De.kt", "w") as f:
    f.write(content)

