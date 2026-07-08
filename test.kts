val text = "name: \"Serde\","
println(text.replaceFirst(Regex("(name:\\s*\"[^\"]*\",)"), "\$1\n    platforms: [.macOS(.v14)],"))
