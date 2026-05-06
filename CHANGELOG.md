# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.4.0] - 2026-05-06

This fork extends the base NetworkStorage plugin with a craftable **storage terminal block**, a fuller **wireless terminal** item workflow, **configurable recipes**, a companion **resource pack** for GUI and custom models, and refined **terminal UX** (feedback, controls, persistence).

### Added

- **Storage terminal block**: Item based on configurable block type (default `OAK_SHELF`) with PDC + Custom Model Data, matching inventory/hotbar model via dedicated resource-pack model.
- **`/storage terminal`**: Gives the terminal block item; permission `networkstorage.give.terminal` (default `op`).
- **Automatic linking**: Placing the terminal block adds it to the placer’s network, applies terminal block state, and enforces terminal limits and permissions without requiring the wand for that step.
- **`networkstorage.terminal`**: Permission to place terminal blocks (separate from give commands).
- **Configurable shaped recipes** in `config.yml` for:
  - Storage network terminal,
  - Wireless network terminal,
  - Storage wand  
  with **`PLANKS`** ingredient resolving to every `_PLANKS` material via `RecipeChoice.MaterialChoice`.
- **Recipe book discovery**: Listeners ensure plugin recipes are discovered when players join and after reload (avoid redundant spam).
- **Language additions**: English + German strings for terminal block, wireless tweaks, deposit guards, and updated GUI hints.
- **Wireless terminal safeguard**: When the GUI was opened via the wireless terminal, that **same** wireless terminal cannot be deposited into the network (still allowed when using a **placed** terminal block).
- **Terminal GUI inventory shortcuts** (see README / in-game lore): granular deposit (single item, half stack, whole stack, all matching stacks) and withdrawal (single, half stack, full stack, as much as fits, bulk withdraw).
- **Persisted terminal preferences**: Each player’s last **sort mode** and **search filter** saved under `plugins/NetworkStorage/player-state.yml` (shared whether opened from block or wireless).
- **Resource pack** (`resourcepacks/networkstorage-test-gui/`): Item overrides for `stick`, `compass`, `oak_shelf`; custom models/textures for wand, wireless terminal, terminal GUI chrome.
- **Pack metadata**: Updated pack format for Minecraft **1.21.x**, pack icon and naming aligned with NetworkStorage branding.
- **`CHANGELOG.md`** and root **`README.md`** describing monorepo layout and publishing notes.

### Changed

- **Wireless terminal material**: Base item **`COMPASS`** (replacing recovery compass); custom texture/model driven by CMD + plugin tag.
- **Storage wand material**: Base item **`STICK`** with handheld-style model alignment to vanilla stick placement.
- **Wireless terminal durability**: **Removed** — unlimited use; lore/strings referencing durability dropped.
- **Feedback routing**: Terminal-centric feedback goes to the **action bar** as plain white text; **search** flow keeps **chat** for prompts and results messaging where intended (general `/storage` help remains chat-oriented).
- **Custom items tooltip titles**: Plugin-introduced items use **white** titles in lore/display while preserving colored lore lines.
- **Recipe parity**: Wireless terminal recipe defaults mirror the storage terminal layout with a configurable alternate ingredient (e.g. ender pearl vs repeater in one slot); fully editable via YAML shape/ingredients.
- **Breaking terminals**: Breaking a **linked** storage terminal drops the **plugin terminal item**, not a plain vanilla shelf (vanilla drops suppressed for that case).
- **Network manager**: Extended `player-state.yml` schema with `terminal-sort-mode` and `terminal-search-filter` alongside existing wireless/active-network selections.

### Fixed

- **Vanilla compass rendering**: Resource-pack `compass.json` retains proper vanilla compass variant fallback (`compass_00` … `compass_31`) so normal compasses are not broken/black-purple when the pack is enabled.

### Removed

- Legacy assumptions tying wireless terminal to **recovery compass** and wand to **blaze rod** in shipped defaults for visuals/recipes (configurable materials remain overridable in `config.yml`).

---

## Earlier versions

Prior iterations existed outside this changelog; treat **1.4.0** as the first fully documented release for **this fork’s** monorepo layout (plugin + scripts + resource pack sources).
