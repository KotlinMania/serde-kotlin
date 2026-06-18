import re

with open("build.gradle.kts.bak", "r") as f:
    content = f.read()

# We want to remove the `kotlin { ... }` block
# Also remove `mavenPublishing { ... }`
# Also remove `tasks.register("swiftExportSmokeTest") { ... }`
# Also remove `val fullTargetBuildTaskNames = ... tasks.named("build") { ... }`

def strip_block(start_marker, content):
    idx = content.find(start_marker)
    if idx == -1: return content
    
    # find the opening brace
    brace_idx = content.find("{", idx)
    if brace_idx == -1: return content
    
    brace_count = 1
    end_idx = brace_idx + 1
    while brace_count > 0 and end_idx < len(content):
        if content[end_idx] == '{':
            brace_count += 1
        elif content[end_idx] == '}':
            brace_count -= 1
        end_idx += 1
        
    return content[:idx] + content[end_idx:]

content = strip_block("kotlin {", content)
content = strip_block("mavenPublishing {", content)
content = strip_block('tasks.register("swiftExportSmokeTest") {', content)
content = strip_block('val fullTargetBuildTaskNames =', content)
content = strip_block('tasks.named("build") {', content)
content = strip_block('tasks.register("test") {', content)
content = strip_block('tasks.register("hostTests") {', content)

with open("build.gradle.kts", "w") as f:
    f.write(content)

