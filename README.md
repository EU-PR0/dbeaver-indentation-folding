# DBeaver Indentation Folding

A third-party Eclipse/DBeaver plug-in that adds indentation-based nested code folding to the DBeaver SQL editor.

## Features

- Folding regions are derived from indentation levels.
- Nested folds are independent.
- Spaces and tabs are supported.
- Blank lines do not break a block.
- Configurable tab width.
- Optional suppression of DBeaver's built-in SQL-structure folding while indentation folding is active.
- Existing collapsed regions are reused where possible while editing.

## Install

After this repository is published with GitHub Pages, use the repository's Pages URL in:

`Help -> Install New Software...`

Example:

`https://YOUR-GITHUB-USER.github.io/dbeaver-indentation-folding/`

Then select **DBeaver SQL Indentation Folding**, finish the wizard, and restart DBeaver.

Settings are available at:

`Window -> Preferences -> Editors -> SQL Editor -> Indentation Folding`

## Compatibility

The current release line is intentionally constrained to the DBeaver bundle API versions verified for:

- DBeaver Community 26.1.5
- DBeaver Community 26.2.0

The CI build resolves against DBeaver's current public p2 repository. If DBeaver changes an internal API outside the allowed bundle ranges, CI is expected to fail until compatibility is reviewed and the ranges are intentionally updated.

## Public p2 repository layout

The GitHub Pages site is a p2 **composite repository**:

```text
/
├── compositeContent.xml
├── compositeArtifacts.xml
├── p2.index
└── releases/
    ├── 1.0.1/
    ├── 1.0.2/
    └── ...
```

Every published version remains available. DBeaver/Eclipse p2 automatically resolves the newest compatible version.

## Development

Requirements:

- JDK 21
- Maven 3.9+
- Internet access to DBeaver/Eclipse p2 repositories

Build:

```bash
mvn -B clean verify
```

Generated p2 repository:

```text
repository/target/repository/
```

## Release

1. Update project versions (for example `1.0.2-SNAPSHOT` / `1.0.2.qualifier`).
2. Commit and push.
3. Create and push matching tag, for example:

```bash
git tag v1.0.2
git push origin v1.0.2
```

The release workflow will:

- build the plug-in with Tycho;
- generate a real p2 repository (`content.jar`, `artifacts.jar`, features and plug-ins);
- preserve all previous versions in the `p2-site` branch;
- regenerate the composite repository;
- publish it to GitHub Pages;
- create a ZIP update-site asset in the GitHub Release.

## License

Apache License 2.0.

This is an independent third-party project and is not affiliated with DBeaver Corporation.
