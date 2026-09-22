# DBeaver Indentation Folding

A third-party Eclipse/DBeaver plug-in that adds indentation-based and explicit region folding to DBeaver SQL editors.

## Features

- Folding regions derived from indentation levels.
- Explicit `#region` / `#endregion` folding independent of indentation.
- Nested explicit regions.
- Region names, for example `#region some name`.
- Folding in regular SQL editors.
- Folding in database object **Source** editors such as procedures, functions, views and triggers.
- Spaces and tabs are supported.
- Blank lines do not break indentation blocks.
- Configurable tab width.
- Independent switches for indentation folding and region folding.
- Optional suppression of DBeaver's built-in SQL-structure folding while plug-in folding is active.
- Existing collapsed regions are reused where possible while editing.

### Region example

```sql
#region first one
    DECLARE var_now DATETIME DEFAULT NOW();

    #region second one
        DECLARE var_id INT;
        DECLARE var_dt DATETIME;
    #endregion
#endregion
```

The `#region` / `#endregion` boundaries are used exactly as explicit folding boundaries and do not depend on indentation.

## Install / update

Use the public p2 Update Site in DBeaver:

`Help -> Install New Software...`

Update Site:

`https://EU-PR0.github.io/dbeaver-indentation-folding/`

Then select **DBeaver SQL Indentation Folding**, finish the wizard, and restart DBeaver.

Existing installations can use:

`Help -> Check for Updates`

Settings are available at:

`Window -> Preferences -> Editors -> SQL Editor -> SQL Folding`

Available switches include:

- **Enable indentation-based folding**
- **Enable #region / #endregion folding**
- **Enable folding in object Source editors (procedures, functions, views, triggers, ...)**
- **Tab width for indentation calculation**
- **Temporarily disable DBeaver SQL-structure folding while plug-in folding is active**

## Compatibility

The current release line is intentionally constrained to DBeaver bundle API versions verified for:

- DBeaver Community 26.1.5
- DBeaver Community 26.2.0
- DBeaver Community 26.2.1

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
    ├── 1.1.0/
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

1. Update project versions (for example `1.1.1-SNAPSHOT` / `1.1.1.qualifier`).
2. Set `RELEASE_VERSION` to the same semantic version.
3. Commit and push to `main`. The release workflow builds and publishes that version automatically.

Tag-based releases are retained for manual recovery/re-publishing.

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
