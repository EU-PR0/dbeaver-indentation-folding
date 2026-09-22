# Changelog

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
