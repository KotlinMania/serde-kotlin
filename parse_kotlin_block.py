with open("build.gradle.kts.bak", "r") as f:
    lines = f.readlines()

in_kotlin = False
brace_count = 0
for i, line in enumerate(lines):
    if line.startswith("kotlin {"):
        in_kotlin = True
        brace_count += line.count('{') - line.count('}')
        print(f"Line {i+1}: {line.strip()}")
        continue
    
    if in_kotlin:
        brace_count += line.count('{') - line.count('}')
        if brace_count == 0:
            print(f"Line {i+1}: {line.strip()} (End of kotlin block)")
            break

