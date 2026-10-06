# DBeaver compatibility

The public update site uses a **selector feature** starting with release **1.2.0**.

Users install one item:

`DBeaver SQL Indentation Folding (Automatic Compatibility)`

The selector does not contain the implementation plug-in directly. It requires
`eu.pro.dbeaver.indentfolding >= 1.0.0`. Eclipse p2 then resolves the newest
implementation whose OSGi `Require-Bundle` constraints are satisfied by the
DBeaver bundles installed in the current application.

This means the decision is based on the actual DBeaver bundle versions, not on
a manually maintained one-to-one mapping between a DBeaver product version and
a plug-in version.

## Verified implementation matrix

| Implementation | Verified DBeaver versions | `org.jkiss.dbeaver.model` | `org.jkiss.dbeaver.ui.editors.sql` |
| --- | --- | --- | --- |
| 1.0.1 | 26.1.5, 26.2.0 | `[2.0.45,2.0.47)` | `[1.0.185,1.0.187)` |
| 1.0.2 | 26.1.5, 26.2.0, 26.2.1 | `[2.0.45,2.0.48)` | `[1.0.185,1.0.188)` |
| 1.1.0 | 26.1.5, 26.2.0, 26.2.1 | `[2.0.45,2.0.48)` | `[1.0.185,1.0.188)` |
| 1.1.1 | 26.1.5, 26.2.0, 26.2.1 | `[2.0.45,2.0.48)` | `[1.0.185,1.0.188)` |
| 1.1.2 | 26.1.5, 26.2.0, 26.2.1, 26.2.2 | `[2.0.45,2.0.49)` | `[1.0.185,1.0.189)` |
| 1.2.0 | 26.1.5, 26.2.0, 26.2.1, 26.2.2 | `[2.0.45,2.0.49)` | `[1.0.185,1.0.189)` |

## Resolver behavior

For example, if a future implementation requires a newer DBeaver SQL Editor
bundle, an older DBeaver installation can keep resolving an older compatible
implementation from the same composite p2 repository.

If no implementation satisfies the installed DBeaver bundle constraints, p2
must reject the installation/update instead of loading an unverified bundle.

Historical release repositories remain available for rollback and dependency
resolution.
