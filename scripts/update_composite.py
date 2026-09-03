#!/usr/bin/env python3
from __future__ import annotations

import argparse
import html
import json
import re
import time
from pathlib import Path
from xml.sax.saxutils import quoteattr

VERSION_RE = re.compile(r"^\d+\.\d+\.\d+(?:[.-][A-Za-z0-9._-]+)?$")

def version_key(value: str):
    parts = re.split(r"[.-]", value)
    key = []
    for part in parts:
        key.append((0, int(part)) if part.isdigit() else (1, part))
    return tuple(key)

def valid_child(path: Path) -> bool:
    metadata = (path / "content.jar").exists() or (path / "content.xml").exists()
    artifacts = (path / "artifacts.jar").exists() or (path / "artifacts.xml").exists()
    return metadata and artifacts

def write_composite(site: Path, versions: list[str]) -> None:
    timestamp = str(int(time.time() * 1000))
    children = "\n".join(
        f"    <child location={quoteattr('releases/' + version + '/')}/>"
        for version in versions
    )

    common_props = f"""  <properties size='3'>
    <property name='p2.timestamp' value='{timestamp}'/>
    <property name='p2.compressed' value='true'/>
    <property name='p2.atomic.composite.loading' value='true'/>
  </properties>"""

    content = f"""<?xml version='1.0' encoding='UTF-8'?>
<?compositeMetadataRepository version='1.0.0'?>
<repository name='DBeaver Indentation Folding'
    type='org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository'
    version='1.0.0'>
{common_props}
  <children size='{len(versions)}'>
{children}
  </children>
</repository>
"""

    artifacts = f"""<?xml version='1.0' encoding='UTF-8'?>
<?compositeArtifactRepository version='1.0.0'?>
<repository name='DBeaver Indentation Folding'
    type='org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository'
    version='1.0.0'>
{common_props}
  <children size='{len(versions)}'>
{children}
  </children>
</repository>
"""

    (site / "compositeContent.xml").write_text(content, encoding="utf-8")
    (site / "compositeArtifacts.xml").write_text(artifacts, encoding="utf-8")
    (site / "p2.index").write_text(
        "version=1\n"
        "metadata.repository.factory.order=compositeContent.xml,\\!\n"
        "artifact.repository.factory.order=compositeArtifacts.xml,\\!\n",
        encoding="utf-8",
    )

def write_landing(site: Path, versions: list[str], repo_slug: str, owner: str) -> None:
    repo_name = repo_slug.split("/", 1)[-1] if "/" in repo_slug else repo_slug
    pages_url = f"https://{owner}.github.io/{repo_name}/" if owner else "(GitHub Pages URL)"
    rows = "\n".join(f"<li><code>{html.escape(v)}</code></li>" for v in reversed(versions))
    latest = versions[-1] if versions else "none"

    page = f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>DBeaver Indentation Folding - p2 Update Site</title>
<style>
body{{font-family:system-ui,-apple-system,Segoe UI,sans-serif;max-width:900px;margin:40px auto;padding:0 20px;line-height:1.55}}
code{{background:#f3f3f3;padding:2px 6px;border-radius:4px}}
.box{{border:1px solid #ddd;border-radius:10px;padding:18px;margin:18px 0}}
</style>
</head>
<body>
<h1>DBeaver Indentation Folding</h1>
<p>Public Eclipse p2 update site for the independent DBeaver SQL indentation-folding plug-in.</p>
<div class="box">
<strong>Install URL</strong>
<p><code>{html.escape(pages_url)}</code></p>
<p>DBeaver: <code>Help -&gt; Install New Software...</code></p>
</div>
<p>Latest published release: <strong>{html.escape(latest)}</strong></p>
<h2>Published versions</h2>
<ul>{rows}</ul>
<p>This project is not affiliated with, endorsed by, or sponsored by DBeaver Corporation.</p>
</body>
</html>
"""
    (site / "index.html").write_text(page, encoding="utf-8")
    (site / "versions.json").write_text(
        json.dumps({"latest": latest, "versions": versions}, indent=2) + "\n",
        encoding="utf-8",
    )

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("site", type=Path)
    parser.add_argument("--repo", default="")
    parser.add_argument("--owner", default="")
    args = parser.parse_args()

    releases = args.site / "releases"
    releases.mkdir(parents=True, exist_ok=True)

    versions = []
    for child in releases.iterdir():
        if child.is_dir() and VERSION_RE.match(child.name) and valid_child(child):
            versions.append(child.name)
    versions.sort(key=version_key)

    write_composite(args.site, versions)
    write_landing(args.site, versions, args.repo, args.owner)
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
