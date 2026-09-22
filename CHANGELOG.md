# Changelog

## 1.1.0

- Added folding to nested database object **Source** editors, including MySQL procedures and functions.
- The Source-editor integration also works for other DBeaver object Source pages based on `SQLSourceViewer` (for example views and triggers).
- Added explicit, indentation-independent `#region ... #endregion` folding.
- Region markers are case-insensitive, may be indented, may contain a name, and may be nested.
- Added a preference to enable/disable region folding independently from indentation folding.
- Added a preference to enable/disable folding in object Source editors.
- Explicit regions take priority over indentation folds when the two ranges would cross, preventing invalid overlapping projection regions.
- Refactored the folding engine so regular SQL editors and object Source editors share the same annotation/session logic.
- Kept compatibility with DBeaver Community 26.1.5, 26.2.0, and 26.2.1.

## 1.0.2

- Added verified compatibility with DBeaver Community 26.2.1 (released 2026-09-21).
- Extended `org.jkiss.dbeaver.model` compatibility through 2.0.47.x.
- Extended `org.jkiss.dbeaver.ui.editors.sql` compatibility through 1.0.187.x.
- Verified that the SQL Editor add-in interface, projection annotation model accessor, document accessor, and built-in folding preference used by the plug-in remain compatible in DBeaver 26.2.1.
- Added an automatic release trigger based on `RELEASE_VERSION` while retaining tag-based releases.

## 1.0.1

- Added support for DBeaver Community 26.2.0 bundle versions.
- Kept compatibility with DBeaver Community 26.1.5.
- Added configurable indentation tab width.
- Added optional temporary suppression of built-in SQL-structure folding.
- Added stale-setting recovery after an abnormal shutdown.

## 1.0.0

- Initial indentation-based SQL folding implementation.
