# Release process

The public site is intentionally append-only at the release-directory level.

## Normal release

For each verified DBeaver compatibility update:

1. Verify the DBeaver bundle versions and the APIs used by the plug-in.
2. Bump the root/module parent version, bundle version and feature version.
3. Set `RELEASE_VERSION` to the same semantic version.
4. Update `CHANGELOG.md` and the compatibility list.
5. Push the release commit to `main`.

A push to `main` that changes `RELEASE_VERSION` automatically starts the release workflow.

The workflow:

- validates all project versions;
- builds with Tycho;
- generates a real p2 repository;
- preserves previous releases in the `p2-site` branch;
- regenerates the composite repository;
- creates the matching Git tag/GitHub Release if needed;
- publishes GitHub Pages.

Tag-based releases (`vX.Y.Z`) and manual `workflow_dispatch` releases are retained for recovery/re-publishing.

## Published layout

Each version is immutable at its own path:

`releases/X.Y.Z/`

The root composite repository keeps all published compatible versions so Eclipse p2/DBeaver can select the newest version whose OSGi requirements match the installed DBeaver bundles.

## Compatibility policy

Do not widen DBeaver `Require-Bundle` ranges merely to make a build pass.

For every new DBeaver bundle line:

1. verify the exact DBeaver stable release;
2. verify the bundle versions used in `MANIFEST.MF`;
3. verify the SQL Editor add-in interface and every DBeaver API directly used by the plug-in;
4. widen only the verified upper bounds;
5. build and publish a new plug-in version.

This prevents a DBeaver update from silently loading the plug-in against an untested incompatible internal API.
