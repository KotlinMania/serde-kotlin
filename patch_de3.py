import re

with open("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/De.kt", "r") as f:
    content = f.read()

# Make sure imports are present
if "import io.github.kotlinmania.serde.core.priv.ContentMapEntry" not in content:
    content = content.replace(
        "import io.github.kotlinmania.serde.core.priv.Content",
        "import io.github.kotlinmania.serde.core.priv.Content\nimport io.github.kotlinmania.serde.core.priv.ContentMapEntry"
    )

# Fix visitContentMapRef and visitContentMap exactly
content = re.sub(
    r'private fun <V> visitContentMapRef\(\s*content: List<Pair<Content, Content>>,\s*visitor: Visitor<V>,\s*\)',
    r'private fun <V> visitContentMapRef(\n    content: List<ContentMapEntry>,\n    visitor: Visitor<V>,\n)',
    content
)

content = re.sub(
    r'private fun <V> visitContentMap\(\s*content: List<Pair<Content, Content>>,\s*visitor: Visitor<V>,\s*\)',
    r'private fun <V> visitContentMap(\n    content: List<ContentMapEntry>,\n    visitor: Visitor<V>,\n)',
    content
)

# Fix flatMapTakeEntry
content = re.sub(
    r'private fun flatMapTakeEntry\(\s*entry: Pair<Content, Content>\?,\s*recognized: List<String>,\s*\): Pair<Content, Content>\? =',
    r'private fun flatMapTakeEntry(\n    entry: ContentMapEntry?,\n    recognized: List<String>,\n): ContentMapEntry? =',
    content
)

# Fix nextPair() signature and usage
content = re.sub(
    r'private fun nextPair\(\): Pair<Content, Content>\? =',
    r'private fun nextPair(): ContentMapEntry? =',
    content
)

# Replace .first and .second on `pair` and `entry`
content = re.sub(r'pair\.first', r'pair.key', content)
content = re.sub(r'pair\.second', r'pair.value', content)
content = re.sub(r'entry\.first', r'entry.key', content)
content = re.sub(r'entry\.second', r'entry.value', content)

# Any other Pair<Content, Content> to replace?
content = content.replace("ArrayList<Pair<Content, Content>>", "ArrayList<ContentMapEntry>")
content = content.replace("cautious<Pair<Content, Content>>", "cautious<ContentMapEntry>")

# Check FlatStructAccess
# In FlatStructAccess, the `found.first` might have been replaced to `found.key` in patch_all.py.
# But just in case:
content = content.replace("found.first", "found.key")
content = content.replace("found.second", "found.value")

with open("serde/src/commonMain/kotlin/io/github/kotlinmania/serde/priv/De.kt", "w") as f:
    f.write(content)

