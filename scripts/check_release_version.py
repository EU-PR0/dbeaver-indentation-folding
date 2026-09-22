#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("version")
    args = parser.parse_args()
    expected = args.version

    root_pom = Path("pom.xml").read_text(encoding="utf-8")
    m = re.search(r"<version>(\d+\.\d+\.\d+)-SNAPSHOT</version>", root_pom)
    if not m:
        raise SystemExit("Cannot find root x.y.z-SNAPSHOT version in pom.xml")

    actual = m.group(1)
    if actual != expected:
        raise SystemExit(f"Release version {expected} does not match project version {actual}")

    release_version_file = Path("RELEASE_VERSION")
    if release_version_file.exists():
        release_version = release_version_file.read_text(encoding="utf-8").strip()
        if release_version != expected:
            raise SystemExit(
                f"RELEASE_VERSION {release_version} does not match requested release {expected}"
            )

    for module_pom in (
        Path("plugins/eu.pro.dbeaver.indentfolding/pom.xml"),
        Path("features/eu.pro.dbeaver.indentfolding.feature/pom.xml"),
        Path("repository/pom.xml"),
    ):
        content = module_pom.read_text(encoding="utf-8")
        if f"<version>{expected}-SNAPSHOT</version>" not in content:
            raise SystemExit(f"{module_pom} parent version does not match release {expected}")

    manifest = Path(
        "plugins/eu.pro.dbeaver.indentfolding/META-INF/MANIFEST.MF"
    ).read_text(encoding="utf-8")
    if f"Bundle-Version: {expected}.qualifier" not in manifest:
        raise SystemExit("MANIFEST.MF Bundle-Version does not match release")

    feature = Path(
        "features/eu.pro.dbeaver.indentfolding.feature/feature.xml"
    ).read_text(encoding="utf-8")
    if f'version="{expected}.qualifier"' not in feature:
        raise SystemExit("feature.xml version does not match release")

    print(f"Release version OK: {expected}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
