import re

def patch_content():
    with open("serde_core/src/commonMain/kotlin/io/github/kotlinmania/serde/core/priv/Content.kt", "r") as f:
        content = f.read()
    
    if "data class ContentMapEntry" not in content:
        # Add ContentMapEntry at the top of the file before sealed interface Content
        content = re.sub(
            r'(sealed interface Content \{)',
            r'data class ContentMapEntry(val key: Content, val value: Content)\n\n\1',
            content
        )
    
    # Replace Pair<Content, Content> with ContentMapEntry in Map
    content = re.sub(
        r'val value: List<Pair<Content, Content>>,',
        r'val value: List<ContentMapEntry>,',
        content
    )
    
    with open("serde_core/src/commonMain/kotlin/io/github/kotlinmania/serde/core/priv/Content.kt", "w") as f:
        f.write(content)

def patch_ser():
    with open("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/Ser.kt", "r") as f:
        content = f.read()
    
    # Replace <Ok, E, S, T> with <Ok, S, T> in serializeTaggedNewtype and serializeUntaggedVariant
    content = re.sub(r'fun <Ok, E, S, T>', 'fun <Ok, S, T>', content)
    
    with open("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/Ser.kt", "w") as f:
        f.write(content)

def patch_de():
    with open("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/De.kt", "r") as f:
        content = f.read()
        
    # Remove `where F : Any` for AdjacentlyTaggedEnumVariantSeed and Visitor
    content = re.sub(r'\) : DeserializeSeed<F>\s*where F : Any \{', r') : DeserializeSeed<F> {', content)
    content = re.sub(r'\) : Visitor<F>\s*where F : Any \{', r') : Visitor<F> {', content)
    
    # Insert FlatMapBuffer class before FlatMapDeserializer
    if "class FlatMapBuffer" not in content:
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
    
    # Update FlatMapDeserializer
    content = re.sub(
        r'class FlatMapDeserializer\(\s*val entries: MutableList<Pair<Content, Content>\?>,\s*\)',
        r'class FlatMapDeserializer(\n    val buffer: FlatMapBuffer,\n)',
        content
    )
    
    # Update flatMapTakeEntry usages in FlatMapDeserializer
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
    
    # Update FlatMapAccess
    content = re.sub(
        r'private class FlatMapAccess\(\s*private val iter: Iterator<Pair<Content, Content>\?>,\s*\) : MapAccess \{',
        r'private class FlatMapAccess(\n    private val buffer: FlatMapBuffer,\n) : MapAccess {\n    private var currentIndex = 0',
        content
    )
    content = re.sub(
        r'while \(iter\.hasNext\(\)\) \{\s*val entry = iter\.next\(\)\s*if \(entry != null\) \{',
        r'while (currentIndex < buffer.size) {\n                val entry = buffer.get(currentIndex++)\n                if (entry != null) {',
        content
    )
    content = re.sub(r'pendingContent = entry\.second', r'pendingContent = entry.value', content)
    content = re.sub(r'seed\.deserialize\(ContentDeserializer\.new\(entry\.first\)\)', r'seed.deserialize(ContentDeserializer.new(entry.key))', content)
    
    # Update FlatStructAccess
    content = re.sub(
        r'private class FlatStructAccess\(\s*private val iter: MutableListIterator<Pair<Content, Content>\?>,\s*private val fields: List<String>,\s*\) : MapAccess \{',
        r'private class FlatStructAccess(\n    private val buffer: FlatMapBuffer,\n    private val fields: List<String>,\n) : MapAccess {\n    private var currentIndex = 0',
        content
    )
    content = re.sub(
        r'while \(iter\.hasNext\(\)\) \{\s*val index = iter\.nextIndex\(\)\s*val entry = iter\.next\(\)\s*if \(entry != null\) \{',
        r'while (currentIndex < buffer.size) {\n                val index = currentIndex\n                val entry = buffer.get(currentIndex++)\n                if (entry != null) {',
        content
    )
    content = re.sub(r'val key = entry\.first', r'val key = entry.key', content)
    content = re.sub(r'pendingContent = entry\.second', r'pendingContent = entry.value', content)
    content = re.sub(r'iter\.set\(null\)', r'buffer.setNull(index)', content)
    
    # Fix flatMapTakeEntry signature and usages
    content = re.sub(
        r'private fun flatMapTakeEntry\(\s*entry: Pair<Content, Content>\?,\s*variants: List<String>,\s*\): Pair<Content, Content>\? =',
        r'private fun flatMapTakeEntry(\n    entry: ContentMapEntry?,\n    variants: List<String>,\n): ContentMapEntry? =',
        content
    )
    content = re.sub(r'val key = entry\.first', r'val key = entry.key', content)
    
    # Fix first.first to first.second
    content = re.sub(r'first\.first to first\.second', r'first.key to first.value', content)

    # Any other Pair<Content, Content>? usages?
    with open("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/De.kt", "w") as f:
        f.write(content)

patch_content()
patch_ser()
patch_de()
