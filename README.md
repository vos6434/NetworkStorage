# Network Storage experiment

Monorepo layout for the **NetworkStorage** Paper plugin plus scripts and an optional **resource pack** for the custom terminal GUI and items.

## Contents

| Path | Purpose |
|------|---------|
| `NetworkStorage/` | Maven project — Paper plugin (Java 21). |
| `resourcepacks/networkstorage-test-gui/` | Resource pack sources (models, textures, font). |
| `*.ps1` | Build, install, dev server, and resource-pack helpers. |
| `.vscode/tasks.json` | IDE tasks (e.g. rebuild + visible server window). |

Runtime Paper server data under `dev-server/` is **not** tracked (see `.gitignore`). Bundle JDK/Maven under `tools/` locally if you use the provided scripts.

## Build

From this folder (PowerShell):

```powershell
.\build-plugin.ps1
```

Install the shaded jar into a Paper plugins folder (example layout):

```powershell
.\install-plugin.ps1
```

See `NetworkStorage/README.md` for gameplay features, commands, and config notes.

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## Publishing to GitHub

This folder already has Git history on branch `main`. Create an empty repository on GitHub (or use your fork as `origin`), then:

```powershell
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
git push -u origin main
```

If you started from someone else’s upstream repo: open that repo on GitHub and click **Fork**, then use **your fork’s** URL as `origin` above.

For a brand-new clone elsewhere without history:

```powershell
git init
git add .
git commit -m "chore: initial import"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
git push -u origin main
```

If you install the [GitHub CLI](https://cli.github.com/), `gh auth login` then `gh repo fork OWNER/REPO` or `gh repo create` can replace some browser steps.
