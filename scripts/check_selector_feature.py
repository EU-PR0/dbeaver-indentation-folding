#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import xml.etree.ElementTree as ET
from pathlib import Path

FEATURE_ID = "eu.pro.dbeaver.indentfolding.feature"
BUNDLE_ID = "eu.pro.dbeaver.indentfolding"


def fail(message: str) -> None:
    raise SystemExit(message)


def main() -> int:
    feature_root = ET.parse(
        "features/eu.pro.dbeaver.indentfolding.feature/feature.xml"
    ).getroot()

    embedded_plugins = feature_root.findall("plugin")
    if embedded_plugins:
        fail(
            "Selector feature must not embed the implementation plug-in; "
            "use <requires><import plugin=...> instead"
        )

    imports = feature_root.findall("./requires/import")
    selector_imports = [
        node for node in imports if node.get("plugin") == BUNDLE_ID
    ]
    if len(selector_imports) != 1:
        fail(f"Expected exactly one selector import for {BUNDLE_ID}")

    selector_import = selector_imports[0]
    if selector_import.get("match") != "greaterOrEqual":
        fail("Selector implementation requirement must use match=greaterOrEqual")
    if selector_import.get("version") != "1.0.0":
        fail("Selector implementation minimum must remain 1.0.0 unless intentionally migrated")

    category_root = ET.parse("repository/category.xml").getroot()

    categorized_feature = False
    for feature in category_root.findall("feature"):
        if feature.get("id") == FEATURE_ID and feature.find("category") is not None:
            categorized_feature = True
            break
    if not categorized_feature:
        fail("Selector feature must be present in the user-facing p2 category")

    bundles = [
        node for node in category_root.findall("bundle")
        if node.get("id") == BUNDLE_ID
    ]
    if len(bundles) != 1:
        fail("Implementation bundle must be included exactly once in category.xml")
    if bundles[0].find("category") is not None:
        fail("Implementation bundle must not be assigned to a user-facing category")

    manifest = Path(
        "plugins/eu.pro.dbeaver.indentfolding/META-INF/MANIFEST.MF"
    ).read_text(encoding="utf-8")
    if "Require-Bundle:" not in manifest:
        fail("Implementation bundle is missing DBeaver Require-Bundle compatibility constraints")

    compatibility = json.loads(Path("compatibility.json").read_text(encoding="utf-8"))
    selector = compatibility.get("selectorFeature", {})
    if selector.get("id") != FEATURE_ID:
        fail("compatibility.json selector feature id does not match")
    if compatibility.get("selectionStrategy") != "latest-compatible-osgi-bundle":
        fail("compatibility.json selection strategy is not latest-compatible-osgi-bundle")

    bundle_version_match = re.search(
        r"^Bundle-Version:\s*(\d+\.\d+\.\d+)\.qualifier\s*$",
        manifest,
        re.MULTILINE,
    )
    if not bundle_version_match:
        fail("Cannot determine current implementation Bundle-Version")
    bundle_version = bundle_version_match.group(1)

    known_versions = {
        item.get("version")
        for item in compatibility.get("implementations", [])
    }
    if bundle_version not in known_versions:
        fail(
            f"Current implementation {bundle_version} is missing from compatibility.json"
        )

    print(
        "Selector feature validation passed: "
        f"{FEATURE_ID} -> latest compatible {BUNDLE_ID}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
