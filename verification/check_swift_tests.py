"""Verify actual Serde Swift behaviors and effective compiler settings."""
import argparse
import json
import re
import shlex
import subprocess
from pathlib import Path

CASES = {
    "testSwiftModuleLoads": "Swift module loads",
    "textHasConcreteSwiftType": "textHasConcreteSwiftType",
    "primitivesHaveConcreteSwiftTypes": "primitivesHaveConcreteSwiftTypes",
    "failureRetainsOriginalDiagnostic": "failureRetainsOriginalDiagnostic",
    "actualDecodeFailureIsTyped": "actualDecodeFailureIsTyped",
}


def verify(text):
    text = re.sub(r"\x1b\[[0-9;]*m", "", text)
    if len(re.findall(r"Test run with 5 tests[^\r\n]*passed", text)) != 1:
        raise ValueError("Expected one passing five-case Serde Swift test summary")
    if re.search(r"(?m)^.*(?:Test|Suite) .*\b(?:failed|skipped)\b", text):
        raise ValueError("Serde Swift test failed or was skipped")
    passing_cases = r"(?m)^.*Test (?:\"[^\"\r\n]+\"|[A-Za-z_]\w*\(\))(?: \(aka '[^\r\n]+'\))? passed\b"
    if len(re.findall(passing_cases, text)) != len(CASES):
        raise ValueError("Unexpected or duplicated Swift case execution")
    for name, description in CASES.items():
        label = r'(?:"' + re.escape(description) + r'"|' + re.escape(name) + r'\(\))'
        alias = r"(?: \(aka '" + re.escape(name) + r"\(\)'\))?"
        if len(re.findall(r"Test " + label + alias + r" passed\b", text)) != 1:
            raise ValueError("Expected exactly one passing execution of " + name)

    frontends = []
    manifests = 0
    for line in text.splitlines():
        if "swift-frontend" not in line or "-module-name " not in line:
            continue
        module = re.findall(r"-module-name\s+(\S+)", line)[-1]
        if module == "main" and "Package.swift" in line:
            manifests += 1
            continue
        modes = re.findall(r"-swift-version\s+(\S+)", line)
        mode = modes[-1] if modes else None
        if "-warnings-as-errors" not in line or not re.search(r"-target\s+arm64-apple-macos(?:x)?15\.0", line):
            raise ValueError("Missing Swift warning/deployment gate: " + module)
        allowed = ["6", "6.0"] if module == "SwiftTestHarnessTests" else ["5", "5.0"]
        if module == "SwiftTestHarnessPackageTests":
            allowed = [None, "6", "6.0"]
        if mode not in allowed:
            raise ValueError("Unexpected effective Swift language mode: " + module)
        frontends.append({"module": module, "swift_language_mode": mode,
                          "warnings_as_errors": True, "macos_deployment_target": "15.0"})
    required = {"Serde", "SerdeCore", "KotlinRuntimeSupport", "KotlinCoroutineSupport", "SwiftTestHarnessTests"}
    if not required <= {row["module"] for row in frontends}:
        raise ValueError("Missing actual dependency/consumer frontend evidence")
    return {"actual_behavior_tests": len(CASES), "failures": 0, "skips": 0,
            "frontends": frontends, "configuration_frontends": manifests,
            "generated_runner_classified_separately": True}


def verify_wrong_payload(text, source, output):
    lines = [line for line in text.splitlines()
             if "swift-frontend" in line and "-module-name SwiftTestHarnessTests" in line]
    if not lines:
        raise ValueError("Missing authored frontend for the real negative consumer")
    tokens = shlex.split(lines[-1])
    frontend = next(Path(token) for token in tokens if token.endswith("/swift-frontend"))
    compiler = frontend.with_name("swiftc")
    flags = []
    keep = {"-I", "-F", "-sdk", "-resource-dir", "-Xcc", "-module-cache-path"}
    for index, token in enumerate(tokens[:-1]):
        if token in keep:
            flags.extend([token, tokens[index + 1]])
    command = [str(compiler), "-typecheck", "-module-name", "SerdeWrongPayloadClient",
               "-swift-version", "6", "-warnings-as-errors", "-target", "arm64-apple-macosx15.0"] + flags + [str(source)]
    process = subprocess.run(command, capture_output=True, text=True)
    log = process.stdout + process.stderr
    output.with_suffix(".log").write_text(log)
    result = {"command": command, "exit_code": process.returncode,
              "rejected_wrong_payload": process.returncode != 0 and bool(
                  re.search(r"cannot convert[^\n]*String[^\n]*Int64", log))}
    output.write_text(json.dumps(result, indent=2) + "\n")
    if not result["rejected_wrong_payload"]:
        raise ValueError("Wrong-payload client was not rejected for its concrete String-to-Int64 mismatch")
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("log", type=Path)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--negative", type=Path)
    args = parser.parse_args()
    result = verify(args.log.read_text())
    output = args.output or args.log.with_suffix(".json")
    if args.negative:
        result["wrong_payload_client"] = verify_wrong_payload(
            args.log.read_text(), args.negative, output.with_name("swift-wrong-payload.json"))
    output.write_text(json.dumps(result, indent=2) + "\n")
    print("Verified five actual Serde Swift behaviors and effective compiler settings")
