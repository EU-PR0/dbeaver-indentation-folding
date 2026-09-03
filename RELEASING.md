# Release process

The public site is intentionally append-only at the release-directory level.

## First public release

1. Create a public GitHub repository, recommended name:
   `dbeaver-indentation-folding`
2. Push this source tree to `main`.
3. In `Settings -> Pages`, set **Source** to **GitHub Actions**.
4. Replace `YOUR-GITHUB-USER` in `pom.xml` and README when convenient.
5. Push tag matching the project version:

```bash
git tag v1.0.1
git push origin v1.0.1
```

The workflow creates the `p2-site` branch automatically and deploys it to GitHub Pages.

## Next release

For 1.0.2 update:

- root and module parent versions from `1.0.1-SNAPSHOT` to `1.0.2-SNAPSHOT`;
- `Bundle-Version` to `1.0.2.qualifier`;
- feature version to `1.0.2.qualifier`;
- changelog.

Then:

```bash
mvn -B clean verify
git commit -am "Prepare 1.0.2"
git tag v1.0.2
git push origin main v1.0.2
```

The new child repository is published to:

`releases/1.0.2/`

and the root composite repository keeps both 1.0.1 and 1.0.2.

## Compatibility policy

Do not widen DBeaver `Require-Bundle` ranges merely to make a build pass.

For every new DBeaver bundle line:

1. verify the APIs used by the plug-in;
2. test install/update in DBeaver;
3. widen only the verified upper range;
4. release a new plug-in version.

This prevents a DBeaver update from silently loading the plug-in against an untested incompatible internal API.
