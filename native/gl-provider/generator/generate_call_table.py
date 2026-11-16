#!/usr/bin/env python3
import argparse
import re
import sys
import xml.etree.ElementTree as ElementTree
from dataclasses import dataclass

MAX_CALL_WORDS = 16

KNOWN_TYPE_NAMES = frozenset({
    "void", "const",
    "GLenum", "GLboolean", "GLbitfield", "GLvoid", "GLbyte", "GLubyte", "GLshort", "GLushort",
    "GLint", "GLuint", "GLsizei", "GLfloat", "GLclampf", "GLdouble", "GLclampd", "GLchar",
    "GLcharARB", "GLhandleARB", "GLintptr", "GLintptrARB", "GLsizeiptr", "GLsizeiptrARB",
    "GLint64", "GLint64EXT", "GLuint64", "GLuint64EXT", "GLsync",
})


@dataclass(frozen=True)
class Command:
    name: str
    return_type: str
    parameter_types: tuple


def expand_braces(pattern):
    group = re.search(r"\{([^{}]*)\}", pattern)
    if group is None:
        return [pattern]
    expanded = []
    for alternative in group.group(1).split(","):
        expanded.extend(expand_braces(pattern[:group.start()] + alternative + pattern[group.end():]))
    return expanded


def read_patterns(path, first_column, last_column):
    names = []
    with open(path, encoding="utf-8") as lines:
        for line in lines:
            for pattern in line.split()[first_column:last_column]:
                names.extend(expand_braces(pattern))
    return names


def declared_type(node):
    text = "".join(node.itertext())
    name = node.find("name").text
    if not text.rstrip().endswith(name):
        raise ValueError(f"cannot separate the type from the name in '{text}'")
    spelled = " ".join(text.rstrip()[:-len(name)].split())
    for identifier in re.findall(r"[A-Za-z_][A-Za-z0-9_]*", spelled):
        if identifier not in KNOWN_TYPE_NAMES:
            raise ValueError(f"'{spelled}' uses a type the call words cannot carry: {identifier}")
    return spelled


def read_registry(path):
    commands = {}
    for node in ElementTree.parse(path).getroot().iter("command"):
        prototype = node.find("proto")
        if prototype is None:
            continue
        name = prototype.find("name").text
        try:
            commands[name] = Command(
                name, declared_type(prototype), tuple(declared_type(parameter) for parameter in node.findall("param")))
        except ValueError:
            continue
    return commands


def thunk(index, command):
    parameters = ", ".join(f"{kind} a{position}" for position, kind in enumerate(command.parameter_types))
    count = len(command.parameter_types)
    lines = [f"{command.return_type} thunk_{command.name}({parameters}) {{"]
    if count == 0:
        words = "std::span<const std::int64_t>{}"
    else:
        packed = ", ".join(f"pack(a{position})" for position in range(count))
        lines.append(f"    const std::array<std::int64_t, {count}> words{{{packed}}};")
        words = "words"
    if command.return_type == "void":
        lines.append(f"    intercept({index}U, {words});")
    else:
        lines.append(f"    return unpack<{command.return_type}>(intercept({index}U, {words}));")
    lines.append("}")
    return lines


def invoker(command):
    count = len(command.parameter_types)
    signature = ", ".join(command.parameter_types)
    words = "words" if count else ""
    arguments = ", ".join(f"unpack<{kind}>(words[{position}])" for position, kind in enumerate(command.parameter_types))
    lines = [
        f"std::int64_t invoke_{command.name}(void* address, std::span<const std::int64_t> {words}) {{",
        f"    const auto function = reinterpret_cast<{command.return_type} (*)({signature})>(address);",
    ]
    if command.return_type == "void":
        lines.append(f"    function({arguments});")
        lines.append("    return 0;")
    else:
        lines.append(f"    return pack(function({arguments}));")
    lines.append("}")
    return lines


def render(commands, intercepted):
    lines = [
        "#include <array>",
        "#include <cstdint>",
        "#include <span>",
        "",
        '#include "calls/call_gate.hpp"',
        '#include "calls/call_table.hpp"',
        '#include "calls/call_words.hpp"',
        '#include "calls/gl_types.hpp"',
        "",
        "namespace minemod::provider {",
        "",
        "namespace {",
        "",
    ]
    for index, command in enumerate(commands):
        if command.name in intercepted:
            lines.extend(thunk(index, command))
            lines.append("")
        lines.extend(invoker(command))
        lines.append("")
    lines.append("const CallRecord records[] = {")
    for command in commands:
        entry = f"thunk_address(&thunk_{command.name})" if command.name in intercepted else "nullptr"
        lines.append(f'    {{"{command.name}", {len(command.parameter_types)}, {entry}, &invoke_{command.name}}},')
    lines.extend(["};", "", "}", "", "std::span<const CallRecord> call_records() noexcept {", "    return records;", "}", "", "}", ""])
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description="Generates the thunks and invokers of the OpenGL call table.")
    parser.add_argument("--registry", required=True)
    parser.add_argument("--calls", required=True)
    parser.add_argument("--extensions", required=True)
    parser.add_argument("--provider-only", required=True)
    parser.add_argument("--output", required=True)
    options = parser.parse_args()

    registry = read_registry(options.registry)
    intercepted = set(read_patterns(options.calls, 0, 1)) | set(read_patterns(options.extensions, 1, None))
    requested = intercepted | set(read_patterns(options.provider_only, 0, 1))

    unknown = sorted(name for name in requested if name not in registry)
    if unknown:
        sys.exit(f"the registry cannot describe these functions with call words: {', '.join(unknown)}")
    commands = [registry[name] for name in sorted(requested)]
    oversized = [command.name for command in commands if len(command.parameter_types) > MAX_CALL_WORDS]
    if oversized:
        sys.exit(f"more than {MAX_CALL_WORDS} arguments: {', '.join(oversized)}")

    with open(options.output, "w", encoding="utf-8", newline="\n") as output:
        output.write(render(commands, intercepted))


if __name__ == "__main__":
    main()
