import os
import re

# Mod.kt fix
mod_path = 'src/commonMain/kotlin/io/github/kotlinmania/serde/core/private/Mod.kt'
with open(mod_path, 'r') as f:
    mod_content = f.read()
mod_content = re.sub(r'import io\.github\.kotlinmania\.serde\.SerdeResult\nimport io\.github\.kotlinmania\.serde\.SerdeResult\n', 'import io.github.kotlinmania.serde.SerdeResult\n', mod_content)
with open(mod_path, 'w') as f:
    f.write(mod_content)

# Fmt.kt fix
fmt_path = 'src/commonMain/kotlin/io/github/kotlinmania/serde/core/ser/Fmt.kt'
with open(fmt_path, 'r') as f:
    fmt_content = f.read()
fmt_content = re.sub(r'SerializeSeq<Unit, SerdeError>', 'SerializeSeq<Unit>', fmt_content)
fmt_content = re.sub(r'SerializeTuple<Unit, SerdeError>', 'SerializeTuple<Unit>', fmt_content)
fmt_content = re.sub(r'SerializeTupleStruct<Unit, SerdeError>', 'SerializeTupleStruct<Unit>', fmt_content)
fmt_content = re.sub(r'SerializeTupleVariant<Unit, SerdeError>', 'SerializeTupleVariant<Unit>', fmt_content)
fmt_content = re.sub(r'SerializeMap<Unit, SerdeError>', 'SerializeMap<Unit>', fmt_content)
fmt_content = re.sub(r'SerializeStruct<Unit, SerdeError>', 'SerializeStruct<Unit>', fmt_content)
fmt_content = re.sub(r'SerializeStructVariant<Unit, SerdeError>', 'SerializeStructVariant<Unit>', fmt_content)
fmt_content = re.sub(r'SerializeSeq<Unit, FmtError>', 'SerializeSeq<Unit>', fmt_content)
fmt_content = re.sub(r'SerializeTuple<Unit, FmtError>', 'SerializeTuple<Unit>', fmt_content)
fmt_content = re.sub(r'SerializeTupleStruct<Unit, FmtError>', 'SerializeTupleStruct<Unit>', fmt_content)
fmt_content = re.sub(r'SerializeTupleVariant<Unit, FmtError>', 'SerializeTupleVariant<Unit>', fmt_content)
fmt_content = re.sub(r'SerializeMap<Unit, FmtError>', 'SerializeMap<Unit>', fmt_content)
fmt_content = re.sub(r'SerializeStruct<Unit, FmtError>', 'SerializeStruct<Unit>', fmt_content)
fmt_content = re.sub(r'SerializeStructVariant<Unit, FmtError>', 'SerializeStructVariant<Unit>', fmt_content)
with open(fmt_path, 'w') as f:
    f.write(fmt_content)

# Doc.kt fix
doc_path = 'src/commonMain/kotlin/io/github/kotlinmania/serde/core/private/Doc.kt'
with open(doc_path, 'r') as f:
    doc_content = f.read()
doc_content = re.sub(r'import io\.github\.kotlinmania\.serde\.SerdeResult\nimport io\.github\.kotlinmania\.serde\.SerdeResult\n', 'import io.github.kotlinmania.serde.SerdeResult\n', doc_content)
with open(doc_path, 'w') as f:
    f.write(doc_content)
