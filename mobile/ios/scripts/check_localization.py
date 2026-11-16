#!/usr/bin/env python3
"""Check locale parity, referenced keys, and literal SwiftUI labels (stdlib only)."""
from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parents[1] / "RoyalReserveBank"
locales = root / "Resources/Localization"
errors = []

def entries(path):
    pairs = re.findall(r'^"([^"]+)"\s*=\s*"((?:\\.|[^"\\])*)";', path.read_text(), re.M)
    keys = [key for key, _ in pairs]
    if len(keys) != len(set(keys)):
        errors.append(f"Duplicate keys: {path}")
    return dict(pairs)

base = entries(locales / "en.lproj/Localizable.strings")
for folder in locales.glob("*.lproj"):
    for table in ("Localizable.strings", "InfoPlist.strings"):
        translated = entries(folder / table)
        expected = entries(locales / "en.lproj" / table)
        if translated.keys() != expected.keys():
            errors.append(f"Key mismatch: {folder / table}")
        for key in expected:
            placeholders = lambda value: sorted(re.findall(r"%(?:\d+\$)?[@df]", value))
            if placeholders(expected[key]) != placeholders(translated.get(key, "")):
                errors.append(f"Placeholder mismatch: {folder.name}: {key}")
for file in root.glob("*.swift"):
    source = file.read_text()
    for key in re.findall(r'"([a-z_]+(?:\.[a-z_]+)+)"\.(?:localized(?:String)?|formatted)', source):
        if key not in base:
            errors.append(f"Unknown localization key: {file.name}: {key}")
    for match in re.finditer(r'\b(?:Text|Button|Label|TextField|Section|Toggle|Picker|LabeledContent|ProgressView)\(\s*"([^"\n]+)"', source):
        tail = source[match.end():]
        if not re.match(r'\.(?:localized|formatted)\b', tail):
            errors.append(f"User-facing literal: {file.name}: {match.group(1)}")
    for match in re.finditer(r'\.(?:navigationTitle|accessibilityLabel|alert)\(\s*"([^"\n]+)"', source):
        if not re.match(r'\.(?:localized|formatted)\b', source[match.end():]):
            errors.append(f"User-facing literal: {file.name}: {match.group(1)}")
if errors:
    print("\n".join(errors))
    sys.exit(1)
print(f"Localization check passed: {len(base)} English keys; locale parity, placeholders and SwiftUI literals checked.")
